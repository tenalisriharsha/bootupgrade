# bootupgrade — Progress

## Vision

`bootupgrade` is an agentic upgrade tool for Spring Boot services. Given a legacy
Java 8 / Spring Boot 2.x Maven project, it upgrades it to Java 21 / Spring Boot 3.x:

1. Run deterministic [OpenRewrite](https://docs.openrewrite.org/) recipes first
   (`javax.*` → `jakarta.*`, deprecated API migrations, Spring Boot 2 → 3
   property/config migrations). These are mechanical, free, and don't need an LLM.
2. Build and run the test suite after **every** step. A step is only kept if the
   build is in a better (or equal) state afterward.
3. Whatever is left — compile errors, failing tests the recipes couldn't fix — is
   handed to an LLM agent with a tight, explicit tool budget (read file, write
   file, run build, nothing else). The agent proposes a patch, the harness
   applies it, rebuilds, and retries, up to a configured iteration cap.
4. The loop stops when the build is green, or the iteration cap is hit — in which
   case the tool reports exactly what it could not fix, instead of pretending to
   succeed.
5. Every run produces: a git branch, a step-by-step changelog (`UPGRADE_LOG.md`),
   and a generated PR description summarizing what changed and why.

The LLM client sits behind an interface (`LlmClient`) with a deterministic fake
implementation (`FakeLlmClient`) used in tests and CI, so the whole pipeline is
testable without any network calls or API keys.

Supporting the core tool:
- **3 realistic legacy sample services** (Java 8 / Spring Boot 2.x Maven
  projects) used as upgrade fixtures and end-to-end test targets.
- **A Spring Boot REST API** that persists upgrade runs and their steps, so
  history survives across invocations.
- **An Angular dashboard** showing runs, step-by-step diffs, and success rate
  across repositories.
- **Terraform** provisioning the API and dashboard on AWS (ECS Fargate, RDS,
  S3 + CloudFront) — validated with `terraform fmt`/`validate`/`plan` and
  `terraform test` with a mock provider, never applied against a real account.

## Architecture

```
bootupgrade/
├── core/                     Java 21 library: the upgrade engine itself
│   ├── model/                 UpgradeRun, UpgradeStep, StepStatus, RunStatus, ChangelogEntry
│   ├── llm/                   LlmClient interface, LlmRequest/Response/ToolCall, FakeLlmClient
│   ├── config/                UpgradeConfig (target versions, iteration caps, tool allow-list)
│   ├── changelog/             ChangelogWriter — renders steps into UPGRADE_LOG.md + PR body
│   ├── recipes/                OpenRewrite recipe runner wrapper (shells out to rewrite-maven-plugin)
│   ├── build/                  Maven build/test runner invoked against target repos
│   ├── git/                    JGit-backed branch creation, commit-per-step, changed-path reporting
│   ├── pipeline/                recipe → commit → build-check orchestration
│   └── agent/                  (Phase 3) tool-limited LLM agent loop + guardrails
│
├── samples/                   Realistic legacy fixtures (Java 8 / Spring Boot 2.x, Maven)
│   ├── legacy-rest-service/    Plain REST CRUD service (javax.servlet, javax.validation)
│   ├── legacy-jpa-service/     JPA/Hibernate persistence service (javax.persistence)
│   └── legacy-security-service/ Spring Security + scheduled jobs (deprecated config style)
│
├── api/                        Spring Boot 3 REST API — persists upgrade runs & steps
│                                (in-memory/H2 for tests, Postgres/RDS in prod)
│
├── dashboard/                  Angular app — runs list, run detail (steps + diffs), success rate
│
├── infra/                      Terraform — ECS Fargate, RDS, S3 + CloudFront
│
├── docs/screenshots/           Captured UI/CLI preview images (Phase 5)
│
└── .github/workflows/          CI (build + test, no deploy)
```

### Core domain model (Phase 1)

- `RunStatus`: `PENDING`, `IN_PROGRESS`, `SUCCEEDED`, `FAILED`, `PARTIAL`
- `StepStatus`: `PENDING`, `SUCCEEDED`, `FAILED`, `SKIPPED`
- `UpgradeStep`: id, name, kind (`RECIPE` / `AGENT_FIX` / `BUILD_CHECK`), status,
  description, diff summary, timestamps
- `UpgradeRun`: id, repository path/name, source/target Java & Spring Boot
  versions, branch name, ordered list of `UpgradeStep`, overall `RunStatus`
- `ChangelogEntry`: one rendered line/section of the human-readable changelog
- `LlmClient`: `LlmResponse complete(LlmRequest request)` — request carries a
  prompt, conversation history, and an allow-list of `ToolDefinition`s; response
  carries either a text answer or a `ToolCall` to execute
- `FakeLlmClient`: deterministic, scripted responses — no network, used in all
  tests and CI
- `UpgradeConfig`: target Java/Spring Boot versions, max agent iterations,
  allowed tool names, repo path
- `ChangelogWriter`: renders an `UpgradeRun`'s steps into Markdown

### Upgrade engine (Phase 2)

- `ProcessRunner` / `DefaultProcessRunner`: abstraction over spawning an
  external process, so `MavenBuildRunner`/`RecipeRunner` are unit-testable
  with a fake instead of a real child process
- `MavenBuildRunner`: runs `mvn clean verify` against a repo, parses the
  Surefire summary into a `BuildResult` (pass/fail counts, raw output)
- `RecipeRunner`: runs `mvn rewrite:run` against a repo, selecting which
  recipes the target repo's `rewrite-maven-plugin` declaration applies
  (`DEFAULT_RECIPES` = `UpgradeSpringBoot_3_0`)
- `GitOperations`: JGit-backed branch creation, commit-all, and
  uncommitted-change reporting — no shell `git` dependency
- `UpgradePipeline`: orchestrates one run — branch, apply recipes, commit,
  build-check — and reports `SUCCEEDED`/`PARTIAL`/`FAILED` based on what
  actually happened, never a false success

## Build Plan

This project is built across **5 sessions**. Each session is a full working
session, not a quick pass — foundation first, polish and previews last.

### Phase 1 — Foundation (Session 1) ✅
- [x] Repo scaffold: all top-level directories (`core`, `samples`, `api`,
      `dashboard`, `infra`, `docs/screenshots`, `.github/workflows`)
- [x] `core` Maven module (Java 21, JUnit 5) with domain model: `RunStatus`,
      `StepStatus`, `UpgradeStep`, `UpgradeRun`, `ChangelogEntry`
- [x] `LlmClient` interface + request/response/tool-call types
- [x] `FakeLlmClient` deterministic implementation
- [x] `UpgradeConfig` with validation
- [x] `ChangelogWriter` rendering Markdown from an `UpgradeRun`
- [x] Minimal CLI entry point (`App`) wired to print version/config, proving the
      module runs end to end
- [x] Unit tests for every class above, all green
- [x] README with Project Status section pointing here
- [x] Clean conventional commit history for the above

### Phase 2 — Upgrade Engine (Session 2) ✅
- [x] OpenRewrite recipe runner wrapper (`core/recipes`): shells out to the
      real `rewrite-maven-plugin` (`org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_0`,
      a composite recipe covering `javax`→`jakarta` plus Spring Boot 2→3
      config/property migrations) against a target repo
- [x] Maven build/test runner (`core/build`): invokes real `mvn clean verify`
      against a target repo, capturing stdout/stderr and exit code, parsing
      the Surefire summary lines into pass/fail/error/skip counts
- [x] Git operations (`core/git`): create upgrade branch, commit per step,
      report changed paths — via JGit, no shell git dependency
- [x] Step orchestration pipeline (`core/pipeline`) wiring recipes → commit →
      build check, reporting `SUCCEEDED`/`PARTIAL`/`FAILED` honestly (never a
      false success when the build is still red)
- [x] 3 realistic legacy sample services scaffolded as real buildable Java 8 /
      Spring Boot 2.x Maven projects (not stubs) with their own tests:
      `legacy-rest-service`, `legacy-jpa-service`, `legacy-security-service`
- [x] Real end-to-end integration test (no fakes) running the full
      recipe+build pipeline against the `legacy-rest-service` fixture —
      actual `mvn rewrite:run` (bumps Boot 2.7→3.0.13, Java 8→17,
      `javax.servlet`/`javax.validation`→`jakarta.*`) followed by a real
      `mvn clean verify` that passes
- [x] Tests green (80/80 in `core`, plus each sample's own suite), CI updated
      to build all 3 samples, conventional commits, PROGRESS.md updated

  Note: `RecipeRunner`/`MavenBuildRunner` are unit-tested with a fake
  `ProcessRunner` (so the default `core` test suite never needs network),
  but the pipeline integration test deliberately uses the real
  implementations end to end — the OpenRewrite/Maven Central dependencies
  it needs resolve and cache in well under a minute.

### Phase 3 — LLM Agent Loop (Session 3)
- [ ] Tool-limited agent loop (`core/agent`): read file, write file, run build —
      nothing else, with a max-iteration guardrail
- [ ] Wire the agent into the pipeline as the fallback after recipes +
      build-check fail
- [ ] Retry loop: agent fix → rebuild → repeat until green or iteration cap hit
- [ ] On cap hit: structured report of exactly what could not be fixed (no
      silent "success")
- [ ] Changelog + PR description generation finalized, covering both recipe
      steps and agent-fix steps
- [ ] At least one sample service upgraded end-to-end (Java 8/Boot 2 →
      Java 21/Boot 3) via the full pipeline, driven by `FakeLlmClient`, as an
      integration test
- [ ] Tests green, conventional commits, PROGRESS.md updated

### Phase 4 — API + Dashboard (Session 4)
- [ ] `api`: Spring Boot 3 REST API persisting `UpgradeRun`/`UpgradeStep`
      (Spring Data JPA; H2 in-memory for tests, Postgres driver for prod — no
      Testcontainers/Docker)
- [ ] API endpoints: list runs, run detail (steps + diffs), success-rate
      aggregation across repos
- [ ] `dashboard`: Angular app — runs list, run detail view (steps, diffs),
      success-rate summary, calling the API
- [ ] API integration tests (in-memory DB) and Angular unit tests
- [ ] Tests green, conventional commits, PROGRESS.md updated

### Phase 5 — Infra, Polish & Ship (Session 5)
- [ ] Terraform (`infra`): ECS Fargate (api), RDS (Postgres), S3 + CloudFront
      (dashboard) — validated with `terraform fmt`/`validate`/`plan` and
      `terraform test` using `mock_provider`; never applied against a real
      account
- [ ] CI workflow green end to end across all modules
- [ ] Full test suite green across `core`, `api`, `dashboard`
- [ ] Capture ≥4-5 real screenshots (dashboard views + CLI runs) into
      `docs/screenshots/`, verified by eye, embedded in README's Preview section
- [ ] README finalized with Preview section
- [ ] `DAILY_REPORT.md` written summarizing the build
- [ ] `STATUS: COMPLETE` below

## Resume point

**Session 2 complete.** Phase 2 is fully done: `core/recipes`, `core/build`,
`core/git`, and `core/pipeline` are implemented and tested (80/80 tests
green in `core`), the 3 legacy sample services are real, independently
buildable Maven projects with their own passing test suites, and one of
them is upgraded end-to-end by a real (no-fakes) integration test. CI now
builds `core` plus all 3 samples.

**Start session 3 here:**
1. `cd core && mvn test` to confirm the Phase 2 baseline is still green
   (should be fast — well under a minute, including the real integration
   test, since Maven Central deps are already cached under `~/.m2`).
2. Build the tool-limited LLM agent loop (`core/agent`): read file, write
   file, run build — nothing else — with a max-iteration guardrail, per the
   Phase 3 checklist below.
3. Wire the agent into `UpgradePipeline` as the fallback when the build is
   still red after the recipe step (`UpgradePipeline.applyRecipes`/
   `checkBuild` in `core/pipeline/UpgradePipeline.java` is where the retry
   loop plugs in — right now a red build after recipes just gets reported
   as `PARTIAL`).
4. Drive at least one sample through the full pipeline (recipes → agent fix
   → green build) in an integration test using `FakeLlmClient` with a
   scripted fix — `legacy-security-service`'s `WebSecurityConfigurerAdapter`
   config is the fixture built specifically for this (OpenRewrite can't
   migrate it automatically; see `samples/legacy-security-service/pom.xml`'s
   description and `SecurityConfig.java`'s javadoc).
5. Finalize changelog/PR description generation to cover agent-fix steps too
   (`ChangelogWriter` already renders any `UpgradeStep`, so this is mostly
   about the agent loop producing good `UpgradeStep` descriptions/diffs).

STATUS: IN_PROGRESS
