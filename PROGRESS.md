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
│   ├── pipeline/                recipe → commit → build-check → agent-fix orchestration
│   └── agent/                   tool-limited LLM agent loop + guardrails
│                                 (ReadFileTool, WriteFileTool, RunBuildTool, AgentFixLoop)
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

### LLM agent fallback (Phase 3)

- `AgentTool`: interface for the three (and only three) things the agent can
  do — `read_file`, `write_file`, `run_build`. Each implementation never
  throws; failures (bad path, missing argument, path traversal, build
  failure) come back as a failed `ToolResult` string fed to the LLM, not an
  exception.
- `ReadFileTool` / `WriteFileTool`: read/overwrite a UTF-8 file at a path
  relative to the repo root; reject any path that resolves outside the repo
  (`RepoPaths.resolveWithinRepo`).
- `RunBuildTool`: wraps `MavenBuildRunner.runVerify`, truncating very long
  output to its tail so one bad build can't blow out the LLM's context.
- `AgentFixLoop`: the retry loop itself. Builds a system+user prompt from
  the initial failing `BuildResult`, offers only the tools in
  `UpgradeConfig.allowedToolNames()`, and for each `LlmResponse` either
  executes the requested tool (feeding the result back as a `TOOL` message
  and looping) or, for a plain-text response, stops immediately and reports
  that as the agent's reason for giving up. A `run_build` call that
  succeeds ends the loop with `AgentFixResult.fixed(...)`; hitting
  `maxAgentIterations` without one ends it with `AgentFixResult.notFixed(...)`
  naming exactly how many iterations were spent and the full tool-call log —
  never a false green.
- `UpgradePipeline` wires it in: a 4-arg constructor takes an optional
  `AgentFixLoop` (`null` disables it, preserving Phase 2 behavior exactly).
  When the post-recipe build is red and an agent is configured with
  `maxAgentIterations() > 0`, the pipeline runs it, commits whatever it
  changed either way, and adds an `AGENT_FIX` step before deciding
  `SUCCEEDED` vs `PARTIAL`.
- Proven against a real fixture, not just mocks: `legacy-security-service`'s
  `SecurityConfig` is migrated by OpenRewrite to the `SecurityFilterChain`
  style but left with an incomplete `InMemoryUserDetailsManager` user
  builder (no password/role — throws at context-startup), and
  `UpgradePipelineAgentFixIntegrationTest` drives the real recipe runner and
  real Maven build against a copy of that fixture, with a `FakeLlmClient`
  scripted to read `SecurityConfig.java`, write the corrected version, and
  rerun the build — ending in a genuinely green `mvn clean verify`.

### API + Dashboard (Phase 4)

- `api` is a standalone Spring Boot 3 Maven project — intentionally no
  compile-time dependency on `core` (a pipeline run reports into it over
  HTTP, not an in-process call). `UpgradeRunEntity`/`UpgradeStepEntity` are
  mutable JPA entities mirroring the shape of `core`'s immutable records;
  `RunStatus`/`StepStatus`/`StepKind` are duplicated as independent enums
  for the same reason.
- `UpgradeRunService` owns id generation, request→entity mapping, and the
  success-rate aggregation (grouped by `repositoryName`, computed from
  `RunStatus` counts) — `UpgradeRunController` stays a thin HTTP layer over
  it. Four endpoints: `POST /api/runs` (record a run + its steps),
  `GET /api/runs` (list, newest first), `GET /api/runs/{id}` (full detail),
  `GET /api/runs/stats/success-rate` (overall + per-repo).
- Default Spring profile uses H2 in-memory (works standalone, no setup); a
  `prod` profile swaps in Postgres driven entirely by `DB_URL`/
  `DB_USERNAME`/`DB_PASSWORD` env vars, for the ECS task Phase 5 will
  define — never applied against a real database from this repo.
- Entity, service, and controller tests all run against the real H2
  in-memory database (no mocking of the persistence layer), including a
  full `MockMvc` pass through `POST` → `GET` list → `GET` detail →
  success-rate for a seeded run.
- `dashboard` is a standalone Angular 18 app (standalone components, no
  NgModules) with two routed views: a runs list (`RunsListComponent`) and
  a run detail (`RunDetailComponent`), both driven by `UpgradeRunService`
  (`HttpClient`) against `environment.apiBaseUrl`. A `StatusBadgeComponent`
  renders every run/step status as an icon + label pill (status color
  reserved per the dataviz palette's good/warning/critical roles, never
  color alone). `SuccessRateComponent` renders the overall rate as a hero
  stat tile plus a proportional stacked bar per repository
  (succeeded/partial/failed segments), with a legend since it's more than
  one series.
- `karma.conf.js` pins `ChromeHeadlessCI` (`--no-sandbox --disable-gpu`,
  `singleRun: true`) so `npm test` runs headless and exits on its own —
  no manual browser flags needed locally or in CI.
- Angular unit tests mock `UpgradeRunService` at the component level (no
  real HTTP) and test the service itself against
  `HttpClientTestingModule`; 17/17 green. CI gained two new jobs (`api`,
  `dashboard`) alongside `core` and `samples`.
- Proven against a live pair, not just unit tests: the API and dashboard
  dev servers were started locally, seeded with five realistic runs across
  the three sample repos (clean recipe-only success, an agent-fix attempt
  still failing, an agent fix that succeeds, and a run that exhausts its
  iteration cap and reports `FAILED` honestly), and every resulting view
  was screenshotted and eyeballed — see the README's Preview section.

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

### Phase 3 — LLM Agent Loop (Session 3) ✅
- [x] Tool-limited agent loop (`core/agent`): read file, write file, run build —
      nothing else, with a max-iteration guardrail
- [x] Wire the agent into the pipeline as the fallback after recipes +
      build-check fail
- [x] Retry loop: agent fix → rebuild → repeat until green or iteration cap hit
- [x] On cap hit: structured report of exactly what could not be fixed (no
      silent "success")
- [x] Changelog + PR description generation finalized, covering both recipe
      steps and agent-fix steps
- [x] At least one sample service upgraded end-to-end (Java 8/Boot 2 →
      Java 21/Boot 3) via the full pipeline, driven by `FakeLlmClient`, as an
      integration test
- [x] Tests green, conventional commits, PROGRESS.md updated

### Phase 4 — API + Dashboard (Session 4) ✅
- [x] `api`: Spring Boot 3 REST API persisting `UpgradeRun`/`UpgradeStep`
      (Spring Data JPA; H2 in-memory for tests, Postgres driver for prod — no
      Testcontainers/Docker)
- [x] API endpoints: list runs, run detail (steps + diffs), success-rate
      aggregation across repos
- [x] `dashboard`: Angular app — runs list, run detail view (steps, diffs),
      success-rate summary, calling the API
- [x] API integration tests (in-memory DB) and Angular unit tests
- [x] Tests green, conventional commits, PROGRESS.md updated

### Phase 5 — Infra, Polish & Ship (Session 5)
- [ ] Terraform (`infra`): ECS Fargate (api), RDS (Postgres), S3 + CloudFront
      (dashboard) — validated with `terraform fmt`/`validate`/`plan` and
      `terraform test` using `mock_provider`; never applied against a real
      account
- [ ] CI workflow green end to end across all modules
- [ ] Full test suite green across `core`, `api`, `dashboard`
- [x] Capture ≥4-5 real screenshots (dashboard views + CLI runs) into
      `docs/screenshots/`, verified by eye, embedded in README's Preview section
      — done early in Session 4 since the dashboard's visible surface was
      already built; revisit only if the UI changes meaningfully in Session 5
- [x] README Preview section in place (done in Session 4, alongside the
      screenshots above) — revisit wording once Phase 5 content lands
- [ ] `DAILY_REPORT.md` written summarizing the build
- [ ] `STATUS: COMPLETE` below

## Resume point

**Session 4 complete.** Phase 4 is fully done: `api` (Spring Boot 3, H2
default / Postgres `prod` profile, no dependency on `core`) persists runs
and steps and serves list/detail/success-rate endpoints, 10/10 tests green
against a real in-memory DB. `dashboard` (Angular 18, standalone
components) has a runs list with a success-rate summary and a run-detail
view, both calling the API; 17/17 unit tests green headless via
`ChromeHeadlessCI` (`karma.conf.js`, `singleRun: true`). `core` is still at
103/103 (unchanged this session). CI gained `api` and `dashboard` jobs
alongside `core` and `samples` — 4 jobs total. The visible surface was
screenshotted against a live, seeded api+dashboard pair and embedded in
the README's Preview section already (5 screenshots, all verified by eye)
— that item does not need to be repeated in Session 5 unless the UI
changes meaningfully.

**Start session 5 here — this is the last session, everything must finish:**
1. Confirm the baseline is still green:
   - `cd core && mvn test` (103 tests)
   - `cd api && mvn test` (10 tests, real H2)
   - `cd dashboard && CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" npx ng test --no-watch --no-progress` (17 tests headless) — or just `npm test`, since `karma.conf.js` now pins `ChromeHeadlessCI` and `singleRun: true`; only set `CHROME_BIN` if Karma can't auto-locate Chrome.
2. Build `infra/`: Terraform for ECS Fargate (running the `api` container),
   RDS Postgres (the `prod` profile's `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`
   targets), and S3 + CloudFront (serving the `dashboard` production
   build). Validate with `terraform fmt`, `terraform validate`, and
   `terraform plan` against a mock/local backend — and `terraform test`
   with `mock_provider` for any logic worth unit-testing (e.g. naming,
   tagging, security-group rules). **Never run `terraform apply` or any
   real AWS CLI/SDK call** — only dummy credentials exist in this
   environment.
3. Make sure the CI workflow (`.github/workflows/ci.yml`) is green end to
   end across all four jobs (`core`, `samples`, `api`, `dashboard`) — add a
   `terraform fmt -check`/`validate` CI job if time allows, matching the
   validation approach above (no `apply`).
4. Re-run the full test suite across every module one more time and fix
   anything red.
5. Decide whether the dashboard/API screenshots need re-capturing — only
   if their UI changed meaningfully since Session 4 (Terraform work has no
   visible surface, so likely no re-capture is needed this session).
6. Finalize `README.md` (status, repository layout, building instructions
   all current) and write `DAILY_REPORT.md` summarizing what was built
   across all 5 sessions, full test results, known limitations (no real
   AWS deploy exercised, no live LLM wired up beyond `FakeLlmClient`,
   dashboard has no auth), and future ideas (real `LlmClient` backed by an
   actual API, CLI→API reporting wired end-to-end, auth on the API/
   dashboard, richer diff rendering).
7. Set `STATUS: COMPLETE` below once everything above is done and green.

STATUS: IN_PROGRESS
