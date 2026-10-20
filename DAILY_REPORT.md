# bootupgrade — Final Report

Five sessions, one project: an agentic tool that upgrades Spring Boot
services from Java 8 / Spring Boot 2.x to Java 21 / Spring Boot 3.x, with
deterministic recipes first and an LLM agent fallback only for what's left,
a persistence layer and dashboard to browse the results, and Terraform to
run it on AWS. **STATUS: COMPLETE.**

## What was built, session by session

**Session 1 — Foundation.** The `core` Java 21 Maven module: the domain
model (`UpgradeRun`, `UpgradeStep`, `RunStatus`, `StepStatus`,
`ChangelogEntry`), the `LlmClient` interface plus a deterministic
`FakeLlmClient` (no network calls needed anywhere in the test suite),
`UpgradeConfig`, `ChangelogWriter`, and a minimal CLI entry point. Every
class got unit tests from the start.

**Session 2 — Upgrade engine.** `RecipeRunner` shells out to the real
`rewrite-maven-plugin` (`UpgradeSpringBoot_3_0`, the composite recipe
covering `javax`→`jakarta` plus Spring Boot 2→3 config migrations).
`MavenBuildRunner` runs real `mvn clean verify` and parses the Surefire
summary. `GitOperations` (JGit, no shell `git`) creates branches and
commits per step. `UpgradePipeline` wires recipe → commit → build-check,
reporting `SUCCEEDED`/`PARTIAL`/`FAILED` honestly — never a false green.
Three realistic legacy Java 8 / Spring Boot 2.x Maven fixtures were built
under `samples/` (`legacy-rest-service`, `legacy-jpa-service`,
`legacy-security-service`), each a real buildable project with its own
tests, not a stub. A real end-to-end integration test runs the actual
pipeline (real OpenRewrite, real Maven) against `legacy-rest-service` and
ends green.

**Session 3 — LLM agent loop.** `core/agent`: exactly three tools —
`read_file`, `write_file`, `run_build` — and nothing else, offered to an
`LlmClient` through `AgentFixLoop`. The loop feeds the agent the build
failure, applies whatever it writes, reruns the build, and repeats up to
`UpgradeConfig.maxAgentIterations()`; hitting the cap without a green build
produces a structured report of exactly what was tried, never a silent
success. `UpgradePipeline` wires it in as the fallback when the post-recipe
build is still red. Proven against a real fixture: OpenRewrite migrates
`legacy-security-service`'s `SecurityConfig` to the `SecurityFilterChain`
style but leaves the in-memory user builder incomplete (no
password/role) — an integration test drives the real recipe runner and
real Maven build with a `FakeLlmClient` scripted to read, fix, and rebuild
the file, ending in a genuinely green `mvn clean verify`.

**Session 4 — API + Dashboard.** `api`, a standalone Spring Boot 3 REST
API (deliberately no compile-time dependency on `core` — a pipeline run
would report into it over HTTP) persisting `UpgradeRun`/`UpgradeStep` via
Spring Data JPA, H2 in-memory by default with a Postgres `prod` profile
driven by `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`. Four endpoints: create a
run, list runs, run detail, success-rate aggregation (overall and per
repository). `dashboard`, a standalone Angular 18 app (standalone
components, no NgModules) with a runs list (success-rate hero stat +
proportional stacked bar per repo) and a run-detail view (every step, its
kind, status, diff summary). Both were started live, seeded with five
realistic runs spanning all three sample repos and every `RunStatus`, and
every view was screenshotted and eyeballed for the README's Preview
section.

**Session 5 — Infra, polish, ship.** `infra/`: four Terraform modules
(`network`, `ecs`, `rds`, `static_site`) wired from a root module —
ECS Fargate running the `api` behind an ALB, RDS Postgres with master
credentials generated straight into Secrets Manager (never a plain
variable or a literal container environment value), and S3 + CloudFront
(OAC, SPA-style 403/404→`index.html` fallback) serving the built Angular
dashboard. Resolved a module dependency cycle (`ecs` needs `rds`'s secret
ARN/endpoint; `rds` wants to restrict ingress to the ECS task security
group, which would point the dependency the other way) by having the
`network` module own both security groups, since neither `ecs` nor `rds`
needs anything back from it. Added `api/Dockerfile`, the image the `ecs`
module's `api_image` variable expects. Validated with `terraform
fmt`/`validate` and 19 `terraform test` cases against a mocked AWS
provider — each module has its own focused suite, plus a root-level
integration suite that proves the cross-module wiring actually works (the
rds secret ARN and endpoint really do reach the ecs task definition, both
land in the subnets the network module meant for them) rather than just
that each module is internally consistent in isolation. CI gained an
`infra` job running the same checks. The full test suite was re-run across
every module as a final check, and `README.md`/`PROGRESS.md` were brought
current.

## Test results (final run, this session)

| Module | Result |
|---|---|
| `core` | 104/104 |
| `api` | 10/10 (real H2 in-memory database) |
| `dashboard` | 17/17 (headless Karma/Jasmine via `ChromeHeadlessCI`) |
| `samples/legacy-rest-service` | 13/13 |
| `samples/legacy-jpa-service` | 13/13 |
| `samples/legacy-security-service` | 6/6 |
| `infra` (`terraform test`, mocked AWS provider) | 19/19 |

**182 checks, all green.** `terraform fmt -check -recursive` and
`terraform validate` also pass at the root and in every module. No
`terraform apply` or AWS CLI/SDK call was ever run — only dummy credentials
exist in this environment, and a `terraform plan` against them fails at
the provider's own `sts:GetCallerIdentity` check before reaching any
resource, which is why the infra test strategy leans on `terraform test`
with `mock_provider` instead.

## Known limitations

- **No real AWS deployment exercised.** The Terraform is validated
  (`fmt`/`validate`/`test` with a mocked provider) but never `plan`ned or
  `apply`ed against a live account — this environment only has dummy
  credentials. A real deployment would also want an HTTPS listener/ACM
  certificate on the ALB (it's HTTP-only today) and a CloudFront custom
  domain, both called out in `infra/README.md`.
- **No live LLM wired up.** Every test and every demoed agent-fix run uses
  `FakeLlmClient`, a deterministic scripted implementation. `LlmClient` is
  a clean interface specifically so a real provider-backed implementation
  is a drop-in, but one was never built or exercised against a live API.
- **The CLI pipeline and the API are not wired together.** A real
  `UpgradeRun` produced by `core/pipeline` is never actually POSTed to
  `api` — the API was seeded directly with realistic sample data for the
  dashboard screenshots and demo. The two were built to the same domain
  shape on purpose, but the HTTP call connecting them doesn't exist yet.
- **No auth anywhere.** Neither `api` nor `dashboard` has any
  authentication or authorization — anyone who can reach the ALB/
  CloudFront URL can read and write upgrade run data.
- **`api/Dockerfile` was never built or run.** The Docker daemon isn't
  available in this environment, so the image the Terraform `ecs` module
  expects was written but never built, pushed, or smoke-tested as a
  container.
- **Single NAT gateway, no multi-AZ RDS.** Deliberate dev-sized
  simplifications — a single point of failure in a real production
  deployment, called out in `infra/README.md`.
- **Diff rendering in the dashboard is a summary string, not a real
  unified diff view** — `UpgradeStep` carries a diff *summary*, not the
  actual patch content, so `dashboard`'s run-detail view shows what
  changed at a sentence level, not a line-by-line diff.

## Future ideas

- A real `LlmClient` implementation backed by an actual model API,
  dropped in behind the existing interface with no changes needed
  elsewhere.
- Wire `core/pipeline` to actually POST each `UpgradeRun`/`UpgradeStep` to
  `api` as it runs, so the dashboard reflects real upgrade attempts instead
  of seeded demo data.
- Auth on `api` (at minimum an API key for writes) and `dashboard` (even a
  simple login), especially before any real deployment.
- Richer diff rendering — store and render the actual unified diff per
  step rather than a one-line summary.
- An HTTPS listener (ACM certificate) on the ALB and a CloudFront custom
  domain for a real deployment, plus splitting the single NAT gateway and
  single-AZ RDS instance into a genuinely multi-AZ setup.
- A CI/CD pipeline that actually builds `api/Dockerfile` and the
  dashboard's production bundle and pushes them to the registry/bucket the
  Terraform `api_image` variable and `dashboard_site` module expect —
  today both are assumed to arrive from a separate, unbuilt pipeline.

## Follow-up: public-readiness pass

Corrections made after the initial five-session build, found by running the tool for real:

- The command line only printed its configuration. It now runs the real pipeline (recipes, commit, build check),
  writes the changelog and PR text, and can report the run to the API with `--report-to`.
- Real Maven output prefixes the Surefire summary with `[INFO]`, so the build step always reported 0 tests. Fixed,
  with a regression test using real output.
- Without a `.gitignore`, the upgrade commit included compiled `target/` files. They are now excluded through
  `.git/info/exclude`, with a regression test.
- The earlier dashboard screenshots used hand-seeded sample data and were described as recorded runs. They are
  replaced by images generated by `scripts/capture`, which runs the real CLI against the sample services and
  screenshots the real dashboard. The scripted-agent screenshots were dropped because no real LLM provider is
  bundled yet.
- Added a LICENSE (MIT), a CI badge, and a README written for visitors.
