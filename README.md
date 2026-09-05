# Training Center Management Application

A web application for managing a training center's full lifecycle: courses,
students, enrollments, scheduling, attendance, payments, grades, and PDF
certificate generation — with three roles (Administrator, Trainer, Student)
and a bonus QR-code attendance feature.

See [`docs/Training Center Management Application.md`](docs/Training%20Center%20Management%20Application.md)
for the original feature brief.

## Tech Stack

| Layer      | Choice |
|------------|--------|
| Backend    | Java 21, Spring Boot 3.x, Spring Web, Spring Security, Spring Data JPA |
| Auth       | JWT (stateless), role-based access control |
| Database   | PostgreSQL 16, Liquibase migrations |
| PDF        | OpenPDF for certificate generation |
| QR Codes   | ZXing |
| Frontend   | React (JavaScript) + Vite, shadcn/ui, React Router, TanStack Query |
| Containerization | Docker + Docker Compose |

Full architecture, conventions, and domain model live in
[`docs/PLAN.md`](docs/PLAN.md).

## Prerequisites

- Docker & Docker Compose
- JDK 21 (for local backend dev without Docker)
- Node 20 (for local frontend dev without Docker)

## Quick Start

```bash
cp .env.example .env
docker compose up --build
```

This brings up `postgres`, `backend`, and `frontend` together, wired
end-to-end:

- Frontend: http://localhost:5173
- Backend health check: http://localhost:8080/api/v1/health → `{"status":"UP"}`

Postgres becomes healthy first (`pg_isready`), then the backend starts and
Liquibase applies its changelogs, then the frontend (nginx, proxying `/api`
to the backend) comes up. Tear everything down, including the database
volume, with:

```bash
docker compose down -v
```

### Default credentials

A bootstrap Administrator account is seeded by Liquibase on first boot so
the system is usable right away:

| Email | Password |
|---|---|
| `admin@tcm.local` | `ChangeMe123!` |

**Rotate this before any real deployment.** Override it per-environment via
the `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD_HASH` env vars (the
password var takes a BCrypt hash, not plaintext) instead of editing the
changelog - see `backend/src/main/resources/application.yml`.

### Authentication

`POST /api/v1/auth/login` (email, password) returns a JWT; send it as
`Authorization: Bearer <token>` on everything else - `/api/v1/auth/**`
(other than `/login`) and `/api/v1/health` are the only public routes.
`GET /api/v1/auth/me` returns the caller's profile. There is no
self-registration endpoint - accounts are created by Administrators (user
management API lands in TCM-8). `CORS_ALLOWED_ORIGIN` (defaults to
`http://localhost:5173`) controls which origin the API accepts
cross-origin requests from.

### Dev mode (hot-reload)

For backend/frontend hot-reload instead of rebuilding images on every
change, opt into `docker-compose.override.yml.example`:

```bash
cp docker-compose.override.yml.example docker-compose.override.yml
docker compose up --build
```

This is dev-only (bind-mounted source, `mvn spring-boot:run` +
spring-boot-devtools for the backend, the Vite dev server for the
frontend) - never used in production.

## Backend Development (without full Docker stack)

To run the backend directly on the host (`mvn spring-boot:run`) against a
real Postgres, without the full stack from `docker-compose.yml` (added in
TCM-5), spin up just the database:

```bash
cp .env.example .env
docker compose -f docker-compose.db.yml up -d
cd backend
mvn spring-boot:run
```

On startup, Liquibase creates its bookkeeping tables
(`databasechangelog`, `databasechangeloglock`) and applies the changelogs
under `src/main/resources/db/changelog/`.

## Frontend Development (without full Docker stack)

To run the frontend directly on the host, without the full stack from
`docker-compose.yml` (added in TCM-5):

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

`VITE_API_BASE_URL` in `frontend/.env` should point at wherever the backend
is running (e.g. `http://localhost:8080/api/v1` for a backend started per
the section above).

## Documentation

- [`docs/PLAN.md`](docs/PLAN.md) — global architecture, conventions, and
  domain model; read this first.
- [`docs/tasks/`](docs/tasks/) — one file per implementation task/branch,
  in dependency order.

## Project Status

This repository is being built incrementally, task by task, following the
roadmap in [`docs/PLAN.md`](docs/PLAN.md#7-ordered-task-list). See the
current task's file under `docs/tasks/` for what's implemented so far.

## License

[MIT](LICENSE)
