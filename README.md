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

**Current phase: Phase 2 — Upgrade Engine, complete.** On top of the Phase 1
domain model, the `core` module now has a real recipe → build-check
pipeline:

- `core/recipes` runs the actual `rewrite-maven-plugin` (the
  `UpgradeSpringBoot_3_0` composite recipe) against a target repository —
  bumping the Spring Boot parent to 3.x, Java to 17+, and renaming
  `javax.*` imports to `jakarta.*`.
- `core/build` shells out to real `mvn clean verify`, parsing the Surefire
  summary out of its output.
- `core/git` (via JGit, no shell `git` dependency) creates the upgrade
  branch and commits each step's changes.
- `core/pipeline` wires them together: apply recipes, commit, build-check,
  and report `SUCCEEDED`/`PARTIAL`/`FAILED` honestly — a build that's still
  red after the recipes is never reported as a success. (Closing that gap
  with an LLM agent fallback is Phase 3.)

Three realistic legacy Java 8 / Spring Boot 2.x Maven services live under
`samples/` as upgrade fixtures, each independently buildable with its own
test suite. One of them, `legacy-rest-service`, is also upgraded end-to-end
by a real integration test — real JGit, a real `mvn rewrite:run`, a real
`mvn clean verify` — no fakes.

The LLM agent loop, the API, the dashboard, and the Terraform infra land in
the sessions that follow.

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
