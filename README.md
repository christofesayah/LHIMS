# LHIMS API - Database Design Baseline

This project currently implements the **database design layer only** using Spring Boot + JPA, following the provided LHIMS ER diagram.

## Added dependencies

- `org.springframework.boot:spring-boot-starter-data-jpa`
- `org.postgresql:postgresql` (runtime)
- `org.hibernate.orm:hibernate-spatial`
- `com.h2database:h2` (test scope)

## Implemented schema model (entities)

- `Region`
- `HealthFacility`
- `FacilityCapacity`
- `RegionalVulnerability`
- `RegionalAccessMetric`
- `ComputedScore`
- `IndicatorDefinition`
- `IndicatorWeight`
- `SimulationScenario`
- `ScenarioAction`
- `ScenarioResult`
- `Role`
- `UserAccount`
- `UserRole`
- `AuditLog`

Enums are under `com.lhims.api.domain.enums`.

## Configuration

Main DB settings are in `src/main/resources/application.properties` and use environment-variable placeholders:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASS`

Test profile uses H2 in-memory DB via `src/test/resources/application.properties`.

## Try it

```bat
mvnw.cmd test
mvnw.cmd spring-boot:run
```

# LHIMS API - Phase 2 Service Layer

This project now includes a role-aware Spring Boot API service layer (controllers + services + repositories) for LHIMS.

## What is implemented

- Domain repositories under `src/main/java/com/lhims/api/repository`
- Service layer under `src/main/java/com/lhims/api/service`
- REST controllers under `src/main/java/com/lhims/api/web`
- JWT auth + role-based guards under `src/main/java/com/lhims/api/security` and `src/main/java/com/lhims/api/config/SecurityConfig.java`
- FastAPI integration client under `src/main/java/com/lhims/api/integration`
- Flyway baseline schema in `src/main/resources/db/migration/V1__baseline.sql`

## Endpoint groups

- `AuthController` -> `/api/auth/*`
- `RegionController` -> `/api/districts/*`
- `ScoreController` -> `/api/districts/*/scores*` and compute endpoints
- `ScenarioController` -> `/api/scenarios/*`
- `FacilityController` -> `/api/facilities/*`
- `UserController` -> `/api/users/*`
- `AuditController` -> `/api/audit/logs*`
- `NotificationController` -> `/api/notifications/*`
- `ExportController` -> `/api/export/*`

## Notes

- Python analytics calls are encapsulated by `AnalyticsClient`.
- If FastAPI is unavailable, compute/recommend calls gracefully degrade to persisted DB data/fallbacks.
- Role checks are enforced with `@PreAuthorize` and JWT authorities.

## Configuration

Main app config: `src/main/resources/application.properties`

- Default mode is **Flyway-first**: schema managed by migrations (`spring.flyway.enabled=true`)
- Hibernate is set to mapping validation only (`spring.jpa.hibernate.ddl-auto=validate`)
- Active migrations are under `src/main/resources/db/migration/` (`V1__baseline.sql`, `V2__add_region_to_simulation_scenarios.sql`, `V3__reconcile_schema_if_missing.sql`, `V4__add_region_to_audit_logs.sql`, `V101__expand_audit_action_type_check.sql`)

Optional seed-data profile: `src/main/resources/application-flyway-seed.properties`

- Keeps Flyway enabled and runs both baseline migrations + seed inserts
- Uses locations `classpath:db/migration,classpath:db/seed`
- Seed script: `src/main/resources/db/seed/V100__seed_dev_test_data.sql`

Key variables:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS`
- `ANALYTICS_BASE_URL`
- `APP_JWT_SECRET`
- `APP_JWT_EXPIRATION_SECONDS`

Test config: `src/test/resources/application.properties`

- Uses H2 + `spring.jpa.hibernate.ddl-auto=create-drop`
- Flyway disabled for tests to avoid PostgreSQL/PostGIS migration mismatch

## Try it

```bat
mvnw.cmd test
mvnw.cmd spring-boot:run
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=flyway-seed
```
