# Legacy sample services

Three realistic Java 8 / Spring Boot 2.x Maven services used as upgrade
fixtures and end-to-end test targets for `bootupgrade`. Scaffolded in Phase 2.

- `legacy-rest-service/` — plain REST CRUD service (`javax.servlet`, `javax.validation`)
- `legacy-jpa-service/` — JPA/Hibernate persistence service (`javax.persistence`)
- `legacy-security-service/` — Spring Security + scheduled jobs, deprecated config style
