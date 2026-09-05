# TCM-4 — Frontend Bootstrap (React + shadcn/ui)

**Branch**: `TCM-4-frontend-bootstrap`
**Depends on**: TCM-1

## Goal

Generate the React (JavaScript, Vite) project with Tailwind CSS and shadcn/ui
wired in, plus the base app shell — no real features yet.

## Steps

1. Scaffold with Vite: `npm create vite@latest frontend -- --template react`
   (JavaScript variant, not TS).
2. Install Tailwind CSS and configure `tailwind.config.js` /
   `postcss.config.js` per shadcn/ui requirements.
3. Run `npx shadcn@latest init` to create `components.json`, set up the
   `src/lib/utils.js` (`cn` helper), base CSS variables/theme in
   `src/index.css`.
4. Add core shadcn components used across the app right away: `button`,
   `input`, `label`, `card`, `table`, `dialog`, `dropdown-menu`, `form`,
   `select`, `badge`, `toast`/`sonner`, `tabs`, `avatar`. Install via
   `npx shadcn@latest add <component>`.
5. Install `react-router-dom`, `@tanstack/react-query`, `axios`.
6. Set up folder skeleton (see `docs/PLAN.md` §3): `src/api/`,
   `src/components/`, `src/features/`, `src/layouts/`, `src/routes/`,
   `src/context/`, `src/lib/`.
7. Add `src/api/client.js`: axios instance with `baseURL` from
   `import.meta.env.VITE_API_BASE_URL`, request interceptor placeholder for
   the auth token (real logic added in `TCM-9`).
8. Build a minimal `App.jsx` with `QueryClientProvider` + `BrowserRouter` and
   one placeholder route (`/`) rendering a "Training Center Management"
   landing card using shadcn `Card`, proving Tailwind + shadcn render
   correctly.
9. Add `.env.example` for frontend: `VITE_API_BASE_URL=http://localhost:8080/api/v1`.
10. Write `frontend/Dockerfile`: multi-stage — `node:20-alpine` build stage
    (`npm ci && npm run build`), then serve the static `dist/` via
    `nginx:alpine` (copy a minimal `nginx.conf` that supports SPA
    fallback to `index.html` and proxies `/api` to the backend service name
    when running in Compose — proxy wiring finalized in `TCM-5`).
11. Add `frontend/.dockerignore` (`node_modules`, `dist`).

## Acceptance Criteria

- `npm run dev` serves the placeholder landing page styled with shadcn/ui.
- `npm run build` produces `dist/` with no errors.
- `docker build -t tcm-frontend frontend/` succeeds and `docker run -p
  5173:80 tcm-frontend` serves the built app.

## Out of Scope

- Auth, routing guards, real pages (later tasks per domain).
- Compose wiring to the backend (`TCM-5`).
