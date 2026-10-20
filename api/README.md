# bootupgrade API

Spring Boot 3 REST API that persists `bootupgrade` upgrade runs and their
steps, so run history survives across invocations. See
[PROGRESS.md](../PROGRESS.md) for the overall build plan.

## Endpoints

| Method | Path                         | Description                                   |
|--------|------------------------------|------------------------------------------------|
| POST   | `/api/runs`                  | Record a completed (or in-progress) run        |
| GET    | `/api/runs`                  | List all runs, newest first                    |
| GET    | `/api/runs/{id}`              | Run detail: metadata + every step              |
| GET    | `/api/runs/stats/success-rate`| Success-rate aggregation, overall + per-repo   |

## Persistence

- `UpgradeRunEntity` / `UpgradeStepEntity` are mutable JPA entities mirroring
  the shape of `core`'s immutable `UpgradeRun` / `UpgradeStep` records (the
  API module intentionally has no compile-time dependency on `core` — a
  pipeline run reports into it over HTTP, not an in-process call).
- Default profile: H2, in-memory, for local runs and tests.
- `prod` profile: real Postgres via `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`
  environment variables (as set by the ECS task definition in `infra/`) —
  never applied against a real database from this repo.

## Running locally

```
cd api
mvn spring-boot:run
```

The API listens on `:8080`. CORS is open to `http://localhost:4200` for the
Angular dev server in `../dashboard`.

## Testing

```
cd api
mvn test
```

Entity, service, and controller tests all run against the real H2 in-memory
database — no mocking of the persistence layer, no Testcontainers/Docker.
