# TCM-10 — Frontend User Management (Admin)

**Branch**: `TCM-10-frontend-user-management`
**Depends on**: TCM-8, TCM-9

## Goal

Admin-facing screens to manage all user accounts (Admins, Trainers,
Students) as generic platform users. Student-specific views come later
(`TCM-15`); this task is the generic account admin console.

## Steps

1. `src/api/userApi.js`: `listUsers(params)`, `getUser(id)`, `createUser`,
   `updateUser`, `setUserStatus`, `resetPassword` — all via TanStack Query
   hooks (`useUsersQuery`, `useCreateUserMutation`, etc.) in
   `src/features/users/hooks.js`.
2. `src/features/users/UsersListPage.jsx` — shadcn `Table` with columns
   (name, email, role, status, created), filter bar (role select, status
   select, search input), pagination controls, "New User" button opening a
   `Dialog`.
3. `src/features/users/UserFormDialog.jsx` — shadcn `Dialog` + `Form` for
   create/edit (role select, status toggle on edit only), validation via
   zod matching backend constraints.
4. Row actions: view detail (`UserDetailSheet.jsx`, shadcn `Sheet`),
   activate/deactivate (confirm via `AlertDialog`), reset password (shows
   the generated temp password once in a dialog).
5. Add "Users" nav item to `AdminLayout` pointing at `/admin/users`.
6. Toast notifications (success/error) for every mutation.

## Acceptance Criteria

- Admin can create a Trainer and a Student account from the UI and see them
  appear in the paginated/filterable list.
- Editing a user's details persists and reflects immediately (query
  invalidation).
- Deactivating a user updates their status badge without a full page
  reload.

## Out of Scope

- Trainer/Student self-service profile editing (kept minimal; can reuse the
  same detail view read-only if desired, but not required here).
