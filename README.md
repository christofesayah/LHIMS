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
