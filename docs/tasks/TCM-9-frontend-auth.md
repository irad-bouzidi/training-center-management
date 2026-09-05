# TCM-9 — Frontend Authentication

**Branch**: `TCM-9-frontend-auth`
**Depends on**: TCM-4, TCM-7

## Goal

Login flow, session persistence, protected/role-aware routing, and the base
authenticated app shell (per-role layout with nav).

## Steps

1. `src/context/AuthContext.jsx`: holds `{ user, token, login(), logout() }`,
   persists token in `localStorage` (`tcm_token`), hydrates on app load by
   calling `GET /api/v1/auth/me` if a token is present.
2. `src/api/authApi.js`: `login(email, password)` → `POST /auth/login`.
3. Wire the axios instance (`src/api/client.js`) request interceptor to
   attach `Authorization: Bearer <token>`, and a response interceptor to
   redirect to `/login` on 401.
4. `src/features/auth/LoginPage.jsx` — shadcn `Card` + `Form` (react-hook-
   form + zod resolver) with email/password fields, submit button with
   loading state, error toast (`sonner`) on failure.
5. `src/routes/ProtectedRoute.jsx` — redirects to `/login` if unauthenticated;
   `src/routes/RoleRoute.jsx` — restricts to an allowed-roles list, else
   redirects to a `403 Forbidden` page or the user's own home.
6. `src/layouts/`: `AdminLayout.jsx`, `TrainerLayout.jsx`,
   `StudentLayout.jsx` — shared shell (sidebar nav via shadcn
   `NavigationMenu`/simple list, top bar with user avatar/menu +
   logout), each with role-appropriate nav items (nav items for
   not-yet-built features can be stubbed/disabled and enabled progressively
   as later tasks land).
7. `src/routes/index.jsx` (or route config in `App.jsx`): `/login` public;
   `/` redirects based on role to `/admin`, `/trainer`, or `/student` home;
   each of those wrapped in `ProtectedRoute` + `RoleRoute` + its layout.
8. Logout clears context + localStorage and redirects to `/login`.

## Acceptance Criteria

- Logging in with the bootstrap admin account lands on an Admin home page
  inside `AdminLayout`.
- Refreshing the page keeps the session (token in localStorage rehydrates
  via `/auth/me`).
- An invalid login shows a toast error and does not navigate.
- Visiting a role-restricted route as the wrong role redirects away.

## Out of Scope

- Self-registration UI (not part of the brief — accounts are admin-created).
- Actual feature pages behind each nav item (built incrementally in later
  tasks).
