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

## Architecture

```
                 browser
                    |
                    v
   +--------------------------------+
   |  frontend  (nginx :80 -> 5173) |   React + Vite build, served static.
   |  /          -> index.html      |   Unknown paths fall through to the
   |  /api/...   -> proxy backend   |   SPA; /api is proxied, so the browser
   +--------------------------------+   only ever talks to one origin.
                    |
                    v
   +--------------------------------+
   |  backend  (Spring Boot :8080)  |   Controller -> Service -> Repository,
   |  JWT filter -> @PreAuthorize   |   DTOs at the edges, ownership rules
   |  Liquibase on startup          |   in the service layer.
   +--------------------------------+
            |                  |
            v                  v
   +-----------------+   +------------------------+
   | postgres :5432  |   | certificates volume    |
   | pg_data volume  |   | generated PDFs on disk |
   +-----------------+   +------------------------+
```

Backend packages mirror the domain: `user`, `course`, `enrollment`,
`schedule`, `attendance`, `payment`, `grade`, `certificate`, `qrattendance`,
`dashboard` — each with its own controller, service, repository, entity and
DTOs. `common` holds the shared error shape and pagination wrapper;
`security` holds the JWT filter chain.

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

### Default accounts

A bootstrap Administrator account is seeded by Liquibase on first boot so
the system is usable right away:

| Email | Password |
|---|---|
| `admin@tcm.local` | `ChangeMe123!` |

**Rotate this before any real deployment.** Override it per-environment via
the `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD_HASH` env vars (the
password var takes a BCrypt hash, not plaintext) instead of editing the
changelog - see `backend/src/main/resources/application.yml`.

#### Demo accounts

With `LIQUIBASE_CONTEXTS=demo` (the default), a sample dataset is seeded too
so there is something to explore immediately: two trainers, four students,
three courses, enrollments in every status, a schedule with a past and a
future, attendance (including one QR check-in), payments in every status,
and a few grades. **Every demo account's password is `ChangeMe123!`.**

| Role | Email | What they'll see |
|---|---|---|
| Administrator | `admin@tcm.local` | Everything: dashboard, users, courses, enrollments, schedule, payments. |
| Trainer | `tina.trainer@tcm.local` | Java Fundamentals — its schedule, rosters to mark, gradebook. |
| Trainer | `tom.teacher@tcm.local` | React in Practice, with sessions still ahead of it. |
| Student | `sam.student@tcm.local` | An approved course, a pending request, grades, and an unpaid invoice. |
| Student | `sara.sassi@tcm.local` | An absence on her record and an overdue invoice. |
| Student | `sofia.benali@tcm.local` | A completed course, fully paid — a certificate can be issued for her. |

No certificates are seeded: a certificate row needs its PDF on disk, which
a changelog can't write into the volume. Issuing one for Sofia from her
student summary is the intended first thing to try.

Set `LIQUIBASE_CONTEXTS=prod` to start with nothing but the administrator.

### Authentication

`POST /api/v1/auth/login` (email, password) returns a JWT; send it as
`Authorization: Bearer <token>` on everything else - `/api/v1/auth/**`
(other than `/login`) and `/api/v1/health` are the only public routes.
`GET /api/v1/auth/me` returns the caller's profile. There is no
self-registration endpoint - accounts are created by Administrators (user
management API lands in TCM-8). `CORS_ALLOWED_ORIGIN` (defaults to
`http://localhost:5173`) controls which origin the API accepts
cross-origin requests from.

### QR attendance

A trainer puts a session's QR code on screen from the attendance page; a
student scans it and is marked present. The code encodes
`FRONTEND_BASE_URL/attend/{sessionId}?token=...`, so **`FRONTEND_BASE_URL`
must be the origin students actually reach the app on** - a code pointing at
`localhost` is unscannable from a phone. For a demo with real devices, set it
to the host's LAN address (e.g. `http://192.168.1.20:5173`) in `.env`.

Codes are short-lived (`QR_VALIDITY_MINUTES`, default 15) and one per session
at a time: showing a new one immediately invalidates the last. `QR_SECRET`
signs them and falls back to `JWT_SECRET` when unset.

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

## Environment variables

All of these live in `.env` (copied from `.env.example`) and are read by
`docker-compose.yml`. The backend's own defaults, used when running it
outside Docker, are in `backend/src/main/resources/application.yml`.

| Variable | Default | What it does |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `tcm` / `tcm` / `changeme` | Database name and credentials. |
| `POSTGRES_PORT` | `5432` | Host port Postgres is published on. Change it if something else already holds 5432. |
| `BACKEND_PORT` | `8080` | Host port for the API. |
| `FRONTEND_PORT` | `5173` | Host port for the web app. |
| `JWT_SECRET` | `changeme` | Signs access tokens. **Rotate before any real deployment.** |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime (24h). |
| `CORS_ALLOWED_ORIGIN` | `http://localhost:5173` | The one origin the API accepts cross-origin requests from. |
| `FRONTEND_BASE_URL` | `http://localhost:5173` | Origin encoded into QR check-in links — must be reachable from a phone (see [QR attendance](#qr-attendance)). |
| `QR_SECRET` | falls back to `JWT_SECRET` | Signs per-session QR tokens. |
| `QR_VALIDITY_MINUTES` | `15` | How long a session's QR code stays valid. |
| `CERTIFICATES_STORAGE_PATH` | `/var/lib/tcm/certificates` | Where generated PDFs are written (a named Docker volume). |
| `CERTIFICATES_MIN_ATTENDANCE_RATE` | `75` | Attendance percentage required before a certificate can be issued. |
| `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD_HASH` | `admin@tcm.local` / hash of `ChangeMe123!` | The first administrator, seeded by Liquibase. The password variable takes a BCrypt hash, not plaintext. |
| `LIQUIBASE_CONTEXTS` | `demo` | Which optional changesets run. `demo` seeds the sample data below; set it to `prod` for a deployment that should start empty. |

## Verifying a deployment

With the stack up, `scripts/smoke.sh` walks every role's core journey
against the running API — login, user and course creation, enrollment and
approval, scheduling and its double-booking rule, attendance (manual and by
QR), grading, payment recording, certificate issue and PDF download, both
dashboards, and the main permission boundaries:

```bash
docker compose up --build -d
./scripts/smoke.sh          # 38 checks; exits non-zero if any fail
```

`frontend/scripts/ui-smoke.mjs` walks the same journeys through a real
browser — logging in as each role, marking a roster, showing the QR code,
downloading a certificate PDF — and fails on any console error:

```bash
cd frontend
npx playwright install chromium   # the browser, downloaded once
node scripts/ui-smoke.mjs         # 26 checks, plus a screenshot per page
```

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `Bind for 127.0.0.1:5432 failed: port is already allocated` | Another Postgres (or another project's stack) holds the port. Set `POSTGRES_PORT=55432` in `.env`, or stop the other container. The same applies to `BACKEND_PORT` / `FRONTEND_PORT`. |
| Backend exits with `Connection to postgres:5432 refused` | Postgres hadn't finished starting. Compose waits for its healthcheck, so this normally self-corrects — `docker compose up` again. |
| Login says "Invalid email or password" with the documented credentials | The database volume predates a change to `BOOTSTRAP_ADMIN_*`. Liquibase only seeds the admin once. `docker compose down -v` and start again. |
| `docker compose up` succeeds but the app shows no data | `LIQUIBASE_CONTEXTS` isn't `demo`, so the sample data was skipped — that's the intended behaviour for `prod`. |
| QR codes scan to a page the phone can't open | `FRONTEND_BASE_URL` points at `localhost`, which on a phone means the phone. Set it to the host's LAN address and restart the backend. |
| Certificate download 500s with "recorded but its PDF is missing" | The certificates volume was removed while its rows survived (e.g. a selective `docker volume rm`). `docker compose down -v` for a clean slate. |
| Schema changes aren't applied | Liquibase never re-runs a changeset it has already recorded. Add a new changelog file rather than editing an old one — see `docs/PLAN.md` §8. |

## Documentation

- [`docs/PLAN.md`](docs/PLAN.md) — global architecture, conventions, and
  domain model; read this first.
- [`docs/tasks/`](docs/tasks/) — one file per implementation task/branch,
  in dependency order.

## Project Status

Feature-complete against the brief. Every task in
[`docs/PLAN.md`](docs/PLAN.md#7-ordered-task-list) — TCM-1 to TCM-30 — is
implemented and merged: authentication and user management, courses,
students and enrollment, scheduling, attendance, payments, grades, PDF
certificates, the QR-attendance bonus feature, and the role dashboards.

## License

[MIT](LICENSE)
