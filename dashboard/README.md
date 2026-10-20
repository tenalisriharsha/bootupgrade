# bootupgrade Dashboard

Angular app showing `bootupgrade` upgrade runs, step-by-step diffs, and
success rate across repositories, calling the [API](../api). See
[PROGRESS.md](../PROGRESS.md) for the overall build plan.

## Views

- **Runs list** (`/`) — every recorded run with its status, Java/Spring
  Boot source→target versions, and step counts, plus a success-rate
  summary: an overall percentage and a per-repository breakdown bar
  (succeeded / partial / failed).
- **Run detail** (`/runs/:id`) — one run's full metadata (branch, versions,
  created-at) and its ordered list of steps, each with its kind, status,
  description, and diff summary.

## Running locally

Start the API first (`cd ../api && mvn spring-boot:run`, listening on
`:8080`), then:

```
cd dashboard
npm start
```

Serves on `http://localhost:4200` and talks to the API via
`environment.development.ts` (`http://localhost:8080/api`). The production
build (`environment.ts`) points at the relative `/api`, meant to be served
behind the same origin/reverse proxy as the API (see `infra/`).

## Testing

```
cd dashboard
npm test
```

Runs the Angular unit test suite headless via Karma/Jasmine (`karma.conf.js`
pins `ChromeHeadlessCI`, `singleRun: true`) — components are tested against
a mocked `UpgradeRunService`, and the service itself against
`HttpClientTestingModule`, so no real API call is made in tests.
