# bootupgrade

An agentic upgrade tool for Spring Boot services. Point it at a legacy
Java 8 / Spring Boot 2.x Maven project and it upgrades it to Java 21 /
Spring Boot 3.x:

1. Runs deterministic [OpenRewrite](https://docs.openrewrite.org/) recipes
   first — `javax.*` → `jakarta.*`, deprecated API migrations, Spring Boot
   2 → 3 property/config migrations.
2. Builds and tests after every step.
3. Hands whatever is left — compile errors, failing tests — to an LLM agent
   with a tight, explicit tool budget (read file, write file, run build),
   retrying until the build is green or it reports exactly what it couldn't
   fix.
4. Produces a branch, a step-by-step changelog, and a generated PR
   description for every run.

The LLM client sits behind an interface with a deterministic fake used in
tests and CI, so the whole pipeline is testable without network access or
API keys.

## Project Status

This project is being built across 5 working sessions, in public. See
**[PROGRESS.md](PROGRESS.md)** for the full architecture, the phased build
plan, and exactly where the project currently stands.

**Current phase: Phase 3 — LLM Agent Loop, complete.** On top of the Phase 2
recipe → build-check pipeline, `core` now has the tool-limited LLM agent
fallback that closes the gap recipes can't:

- `core/recipes` runs the actual `rewrite-maven-plugin` (the
  `UpgradeSpringBoot_3_0` composite recipe) against a target repository —
  bumping the Spring Boot parent to 3.x, Java to 17+, and renaming
  `javax.*` imports to `jakarta.*`.
- `core/build` shells out to real `mvn clean verify`, parsing the Surefire
  summary out of its output.
- `core/git` (via JGit, no shell `git` dependency) creates the upgrade
  branch and commits each step's changes.
- `core/agent` is the fallback: three tools and nothing else —
  `read_file`, `write_file`, `run_build` — offered to an `LlmClient`
  through `AgentFixLoop`, which feeds it the build failure, applies
  whatever it writes, reruns the build, and repeats up to
  `UpgradeConfig.maxAgentIterations()`. If the cap is hit without a green
  build, it reports exactly what was tried instead of claiming success.
- `core/pipeline` wires all of it together: apply recipes, commit,
  build-check, then — only if the build is still red and an agent is
  configured — the agent-fix step, reporting `SUCCEEDED`/`PARTIAL`/`FAILED`
  honestly throughout.

Three realistic legacy Java 8 / Spring Boot 2.x Maven services live under
`samples/` as upgrade fixtures, each independently buildable with its own
test suite. Two of them are upgraded end-to-end by real integration tests
(real JGit, a real `mvn rewrite:run`, a real `mvn clean verify` — no fakes
except the LLM itself): `legacy-rest-service` goes green on recipes alone,
while `legacy-security-service` needs the agent fallback — OpenRewrite
migrates its `WebSecurityConfigurerAdapter` to the `SecurityFilterChain`
style but leaves the in-memory user builder incomplete (no password/role),
which a scripted `FakeLlmClient` then reads, fixes, and rebuilds to green.

The API, the dashboard, and the Terraform infra land in the sessions that
follow.

## Repository layout

```
core/        Java 21 library: domain model, LLM client, recipes, build runner, git, pipeline
samples/     Realistic legacy Java 8 / Spring Boot 2.x Maven services used as upgrade fixtures
api/         Spring Boot 3 REST API persisting upgrade runs & steps
dashboard/   Angular app: runs, step diffs, success rate across repos
infra/       Terraform: ECS Fargate, RDS, S3 + CloudFront
docs/        Documentation assets (screenshots, etc.)
```

## Building

```
cd core
mvn test
```

Each sample under `samples/` is an independent Maven project and can be
built the same way:

```
cd samples/legacy-rest-service
mvn test
```

## License

Not yet decided.
