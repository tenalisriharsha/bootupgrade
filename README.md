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

**Current phase: Phase 1 — Foundation, complete.** The `core` module
(domain model, `LlmClient`/`FakeLlmClient` abstraction, config, changelog
rendering, and a minimal CLI) is implemented with a full passing test suite.
The upgrade engine itself (OpenRewrite recipes, build runner, LLM agent
loop), the sample legacy services, the API, the dashboard, and the
Terraform infra land in the sessions that follow.

## Repository layout

```
core/        Java 21 library: the upgrade engine (domain model, LLM client, config, changelog)
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

## License

Not yet decided.
