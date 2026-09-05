# TCM-2 — Backend Bootstrap (Spring Boot)

**Branch**: `TCM-2-backend-bootstrap`
**Depends on**: TCM-1

## Goal

Generate the Spring Boot skeleton with the package layout defined in
`docs/PLAN.md`, wired for Controller/Service/Repository architecture, with a
health-check endpoint and a Dockerfile — no domain logic yet.

## Steps

1. Generate a Spring Boot 3.x project (via start.spring.io or manually) into
   `backend/` with:
   - Group: `com.tcm`, artifact: `tcm-backend`, packaging: jar, Java 21.
   - Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`,
     `spring-boot-starter-validation`, `spring-boot-starter-security`,
     `postgresql` driver, `liquibase-core`, `lombok`, `springdoc-openapi-
     starter-webmvc-ui` (Swagger UI), `spring-boot-starter-test`.
2. Create the base package structure under
   `backend/src/main/java/com/tcm/`:
   - `TcmApplication.java` (main class).
   - `config/` (empty placeholder `WebConfig.java` with CORS stub disabled
     for now).
   - `common/` package with:
     - `ApiError.java` (DTO: timestamp, status, error, message, path).
     - `GlobalExceptionHandler.java` (`@RestControllerAdvice`, handles
       `MethodArgumentNotValidException`, `EntityNotFoundException`,
       generic `Exception` → 500).
     - `ResourceNotFoundException.java`, `BadRequestException.java`.
3. Add `backend/src/main/resources/application.yml` with:
   - `server.port: 8080`
   - `spring.datasource.url/username/password` read from env vars
     (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`) with local defaults.
   - `spring.jpa.hibernate.ddl-auto: validate` (Liquibase owns schema).
   - `spring.liquibase.enabled: true` (changelog wired in `TCM-3`).
4. Add a trivial `HealthController` at `/api/v1/health` returning
   `{ "status": "UP" }` — this is the only controller in this task, purely to
   prove the app boots and routes work.
5. Write `backend/Dockerfile`:
   - Multi-stage build: `maven:3.9-eclipse-temurin-21` build stage running
     `mvn -B clean package -DskipTests`, then a slim
     `eclipse-temurin:21-jre` runtime stage copying the built jar,
     `EXPOSE 8080`, `ENTRYPOINT ["java","-jar","/app/app.jar"]`.
6. Add `backend/.dockerignore` (`target/`, `.git`, `*.md`).
7. Verify locally: `cd backend && mvn clean package` succeeds, and
   `curl localhost:8080/api/v1/health` (after running the jar with a
   temporary local Postgres, or with datasource disabled for this test)
   returns 200.

## Acceptance Criteria

- `mvn clean package` builds a runnable jar.
- Package layout matches `docs/PLAN.md` §3.
- `GET /api/v1/health` returns `{ "status": "UP" }`.
- `docker build -t tcm-backend backend/` succeeds.

## Out of Scope

- Real datasource/Liquibase wiring against a running Postgres (`TCM-3`).
- Security configuration (`TCM-7`).
- Any domain entity.
