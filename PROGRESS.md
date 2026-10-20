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
│   ├── recipes/                (Phase 2) OpenRewrite recipe runner wrapper
│   ├── build/                  (Phase 2) Maven build/test runner invoked against target repos
│   ├── agent/                  (Phase 3) tool-limited LLM agent loop + guardrails
│   └── git/                    (Phase 2) branch creation, commit-per-step, diff capture
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

### Phase 2 — Upgrade Engine (Session 2)
- [ ] OpenRewrite recipe runner wrapper (`core/recipes`): run a fixed recipe
      list (`javax`→`jakarta`, Spring Boot 2→3 config/property migrations)
      against a target repo
- [ ] Maven build/test runner (`core/build`): invoke `mvn verify` against a
      target repo, capture stdout/stderr, exit code, and parse pass/fail
- [ ] Git operations (`core/git`): create upgrade branch, commit per step,
      capture diffs — via JGit, no shell git dependency
- [ ] Step orchestration pipeline wiring recipes → build check → (stop if green)
- [ ] 3 realistic legacy sample services scaffolded as real buildable Java 8 /
      Spring Boot 2.x Maven projects (not stubs) with their own tests
- [ ] Integration tests running the recipe+build pipeline against the sample
      services with `FakeLlmClient`
- [ ] Tests green, conventional commits, PROGRESS.md updated

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

**Session 1 complete.** Phase 1 is fully done: repo scaffold, `core` module with
domain model, `LlmClient`/`FakeLlmClient`, `UpgradeConfig`, `ChangelogWriter`,
minimal CLI, all with passing unit tests, plus README and this file.

**Start session 2 here:**
1. `cd core && mvn test` to confirm the Phase 1 baseline is still green.
2. Build `core/recipes` (OpenRewrite wrapper), `core/build` (Maven runner),
   `core/git` (JGit wrapper) per the Phase 2 checklist above.
3. Scaffold the 3 real legacy sample services under `samples/` — they must be
   genuinely buildable Java 8 / Spring Boot 2.x Maven projects, not stubs.
4. Wire the recipe → build-check pipeline and add integration tests against the
   sample services.

STATUS: IN_PROGRESS
