# Legacy sample services

Three realistic Java 8 / Spring Boot 2.x Maven services used as upgrade
fixtures and end-to-end test targets for `bootupgrade`. Each is an
independent, fully buildable Maven project with its own test suite — run
`mvn test` inside any of them. Each also declares the `rewrite-maven-plugin`
with the `rewrite-spring` recipe dependency, so `bootupgrade`'s
`core/recipes` module can run `mvn rewrite:run` against them directly.

- `legacy-rest-service/` — plain REST CRUD service (`javax.servlet` request filter,
  `javax.validation` on request DTOs). Upgraded end-to-end by
  `UpgradePipelineIntegrationTest` in `core`.
- `legacy-jpa-service/` — JPA/Hibernate persistence service (`javax.persistence`
  entity + Spring Data repository), tested against an in-memory H2 database.
- `legacy-security-service/` — Spring Security configured via the deprecated
  `WebSecurityConfigurerAdapter` style, plus a `@Scheduled` job. The security
  config is exactly the kind of migration OpenRewrite can't fully automate —
  left as fixture material for the Phase 3 LLM agent fallback.
