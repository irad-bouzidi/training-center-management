# TCM-3 — Database & Liquibase Setup

**Branch**: `TCM-3-database-liquibase-setup`
**Depends on**: TCM-2

## Goal

Wire the backend to a real PostgreSQL instance and stand up the Liquibase
changelog structure that every future migration will extend. No domain
tables yet beyond a minimal smoke-test table used to prove the pipeline
works (removed or kept as the first real migration in `TCM-6`).

## Steps

1. Add `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
   that `include`s files from `db/changelog/changes/` in order.
2. Create `backend/src/main/resources/db/changelog/changes/` directory
   convention: filenames `YYYYMMDD-NN-description.yaml` (e.g.
   `20260101-01-init-extensions.yaml`).
3. First changelog file: enable `pgcrypto` or `uuid-ossp` extension (for
   `gen_random_uuid()` / `uuid_generate_v4()` used as default for UUID PKs).
4. Configure `spring.liquibase.change-log:
   classpath:db/changelog/db.changelog-master.yaml` in `application.yml`.
5. Add `application-docker.yml` profile with datasource URL pointing at the
   `postgres` service hostname (used when running via Docker Compose):
   `jdbc:postgresql://postgres:5432/${POSTGRES_DB}`.
6. Provide a local dev convenience: `docker-compose.db.yml` (or document in
   README) to spin up just Postgres for running the backend outside Docker
   during development — `docker run -e POSTGRES_DB=... postgres:16`.
7. Verify: start Postgres locally, run
   `mvn spring-boot:run`, confirm Liquibase creates its bookkeeping tables
   (`databasechangelog`, `databasechangeloglock`) plus the extension change,
   with no errors.
8. Add a `backend/src/test/resources/application-test.yml` profile for
   integration tests using Testcontainers Postgres (dependency
   `org.testcontainers:postgresql` + `org.testcontainers:junit-jupiter`
   added to `pom.xml`), so later tasks' integration tests have a ready
   pattern to follow.

## Acceptance Criteria

- App boots against a real Postgres with `spring.jpa.hibernate.ddl-auto:
  validate` and no schema errors.
- `databasechangelog` table shows the extension changeset applied.
- A minimal Testcontainers-based `@SpringBootTest` passes, proving the test
  DB bootstrap pattern works for future domain tests.

## Out of Scope

- Any business table (first one lands in `TCM-6`).
- Docker Compose full-stack orchestration (`TCM-5`).
