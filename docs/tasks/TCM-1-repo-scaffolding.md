# TCM-1 — Repository Scaffolding

**Branch**: `TCM-1-repo-scaffolding`
**Depends on**: none (first task)

## Goal

Set up the monorepo skeleton (folders, root config, tooling) that every later
task builds on. No application code yet — just the shape of the repo.

## Steps

1. Create top-level folders: `backend/`, `frontend/`, `docs/` (already
   present from planning).
2. Add root `.gitignore` covering: Java/Maven (`target/`, `*.class`), Node
   (`node_modules/`, `dist/`, `build/`), IDE files (`.idea/`, `.vscode/`),
   env files (`.env`, `.env.local`), OS files (`.DS_Store`).
3. Add root `.editorconfig` (UTF-8, LF line endings, 2 spaces for
   JS/JSON/YAML, 4 spaces for Java).
4. Add root `.env.example` with placeholders that later tasks will fill in:
   `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`,
   `BACKEND_PORT`, `FRONTEND_PORT`, `JWT_SECRET`, `JWT_EXPIRATION_MS`.
5. Write the root `README.md`:
   - Project name/description (from the feature brief).
   - Tech stack summary (link to `docs/PLAN.md`).
   - Prerequisites (Docker, Docker Compose, JDK 21, Node 20 — for local dev
     without Docker).
   - Quick start: `cp .env.example .env && docker compose up --build`.
   - Link to `docs/PLAN.md` and `docs/tasks/` for the implementation roadmap.
6. Restore/finalize the deleted `readme.md` at repo root as the canonical
   `README.md` (git status shows `readme.md` deleted — replace it with the
   new `README.md`, matching case used by the rest of the plan).
7. Set up a root `LICENSE` file (MIT, unless the user specifies otherwise —
   flag this as a placeholder to confirm with the user).
8. Initialize a basic GitHub Actions or generic CI placeholder is **out of
   scope** for this task (handled implicitly by later Docker/build tasks); do
   not add CI yet.

## Acceptance Criteria

- `backend/`, `frontend/`, `docs/` directories exist.
- `.gitignore`, `.editorconfig`, `.env.example`, `README.md`, `LICENSE` exist
  at repo root.
- `git status` is clean after `git add -A && git commit`.
- No build tooling is configured yet (that's `TCM-2` and `TCM-4`).

## Out of Scope

- Spring Boot project generation (`TCM-2`).
- React project generation (`TCM-4`).
- Docker Compose file (`TCM-5`).
