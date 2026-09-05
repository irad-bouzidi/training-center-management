# TCM-8 — User Management API

**Branch**: `TCM-8-user-management-api`
**Depends on**: TCM-7

## Goal

Full CRUD for user accounts (Admins, Trainers, Students), restricted to the
Administrator role, following Controller → Service → Repository.

## Steps

1. In `com.tcm.user`, add:
   - `dto/UserRequest.java` (firstName, lastName, email, password [only on
     create], phone, role) with Bean Validation annotations.
   - `dto/UserResponse.java` (id, firstName, lastName, email, phone, role,
     status, createdAt) — never expose `password_hash`.
   - `mapper/UserMapper.java` (manual or MapStruct) entity↔DTO.
   - `UserService.java` interface + `UserServiceImpl.java`: `create`,
     `update`, `changeStatus` (activate/deactivate), `findById`, `search`
     (paginated, filterable by role/status/name), `delete` (soft delete via
     status, not a hard DB delete).
   - `UserController.java`:
     - `POST /api/v1/users` (ADMIN only) — create Trainer/Student/Admin
       accounts, hashing password via `PasswordEncoder`.
     - `GET /api/v1/users` (ADMIN only) — paginated list with filters.
     - `GET /api/v1/users/{id}` (ADMIN, or self).
     - `PUT /api/v1/users/{id}` (ADMIN, or self for limited fields — enforce
       in service layer that non-admins can't change `role`/`status`).
     - `PATCH /api/v1/users/{id}/status` (ADMIN only) — activate/deactivate.
     - `POST /api/v1/users/{id}/reset-password` (ADMIN only) — sets a new
       temp password.
2. Enforce `@PreAuthorize("hasRole('ADMIN')")` at controller level for
   admin-only endpoints; self-access endpoints check
   `authentication.principal.id == #id` via a `@PreAuthorize` SpEL
   expression or in the service.
3. Tests: `UserServiceImplTest` (unit, mocked repository) and
   `UserControllerIT` (Testcontainers) covering create/list/update/status
   change and the 403 case for a Trainer/Student hitting admin-only routes.

## Acceptance Criteria

- Admin can create, list (paginated/filterable), view, update, deactivate
  users of any role.
- Non-admin users get 403 on admin-only routes and can only view/edit their
  own profile via the self-access routes.
- Passwords are never returned in any response body.

## Out of Scope

- Frontend UI (`TCM-10`).
- Student-specific extra profile fields (`TCM-13`).
