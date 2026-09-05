# TCM-5 — Docker Compose Orchestration

**Branch**: `TCM-5-docker-compose-orchestration`
**Depends on**: TCM-2, TCM-3, TCM-4

## Goal

Bring `postgres`, `backend`, and `frontend` up together with one command,
fully wired end-to-end, closing out Phase 0.

## Steps

1. Write root `docker-compose.yml` with three services:
   - `postgres`: image `postgres:16-alpine`, env from root `.env`
     (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`), named volume
     `pg_data:/var/lib/postgresql/data`, healthcheck via `pg_isready`.
   - `backend`: build context `./backend`, `depends_on: postgres:
     condition: service_healthy`, env vars for datasource pointing at
     `postgres` service name, `SPRING_PROFILES_ACTIVE=docker`, port mapping
     `${BACKEND_PORT:-8080}:8080`.
   - `frontend`: build context `./frontend`, `depends_on: backend`, port
     mapping `${FRONTEND_PORT:-5173}:80`, build arg
     `VITE_API_BASE_URL=/api/v1` (frontend nginx proxies `/api` → backend).
2. Finalize `frontend/nginx.conf`: SPA fallback (`try_files $uri /index.html`)
   plus a `location /api/ { proxy_pass http://backend:8080/api/; }` block so
   the browser only ever talks to the frontend origin.
3. Add a top-level `networks: tcm-network` shared by all services.
4. Wire `.env` (from `.env.example`) as the single source of ports/creds for
   Compose; document required vars in README.
5. Add `docker-compose.override.yml` (optional, dev-only, gitignored or
   checked in as an example) enabling backend hot-reload via a bind mount
   and `spring-boot-devtools`, and Vite dev server instead of nginx for
   frontend — clearly documented as a dev convenience, not used in prod.
6. Update root `README.md` "Quick start" section with the real, verified
   command sequence and the URLs to hit (`http://localhost:5173`,
   `http://localhost:8080/api/v1/health`).
7. Verify end-to-end: `docker compose up --build`, confirm:
   - Postgres becomes healthy.
   - Backend logs show Liquibase applying changesets successfully.
   - `curl http://localhost:8080/api/v1/health` → 200.
   - Frontend served at `http://localhost:5173` shows the placeholder page.

## Acceptance Criteria

- `docker compose up --build` starts all three services with no manual
  steps beyond `cp .env.example .env`.
- Backend successfully connects to Postgres and applies migrations.
- Frontend is reachable and (once wired) can reach the backend through the
  nginx proxy.
- `docker compose down -v` cleanly tears everything down.

## Out of Scope

- Production-grade secrets management (documented as a follow-up, not
  implemented here).
- CI/CD pipeline.
