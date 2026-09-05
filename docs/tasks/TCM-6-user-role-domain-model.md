# TCM-6 — User & Role Domain Model

**Branch**: `TCM-6-user-role-domain-model`
**Depends on**: TCM-3

## Goal

Introduce the `users` table (see `docs/PLAN.md` §5) and the JPA entity +
repository layer for it. This is the first real domain migration and the
template every later domain package follows.

## Steps

1. Add Liquibase changelog
   `db/changelog/changes/<date>-01-create-users-table.yaml` creating
   `users` exactly per the `docs/PLAN.md` schema (`id UUID default
   gen_random_uuid()`, `role` and `status` as Postgres enums or
   varchar+check-constraint — prefer varchar + check constraint for easier
   future value additions), plus a unique index on `email`.
2. Include the new file from `db.changelog-master.yaml`.
3. Create `com.tcm.user` package:
   - `model/User.java` — JPA entity implementing Spring Security's
     `UserDetails` is deferred to `TCM-7`; for now, a plain `@Entity` with
     fields matching the table, `@Enumerated(EnumType.STRING)` for `role`
     (`Role` enum: `ADMIN`, `TRAINER`, `STUDENT`) and `status`
     (`UserStatus`: `ACTIVE`, `INACTIVE`).
   - `UserRepository.java extends JpaRepository<User, UUID>` with
     `Optional<User> findByEmail(String email)` and
     `boolean existsByEmail(String email)`.
4. Add `data.sql`-free seed: instead, add a Liquibase changeset inserting one
   bootstrap Administrator account (email/password read as changelog
   properties, password pre-hashed with BCrypt at authoring time) so the
   system is usable on first boot. Document the default credentials in
   README (and flag them for rotation).
5. Write a repository-layer test (`@DataJpaTest` + Testcontainers) verifying
   `findByEmail` and the unique-email constraint.

## Acceptance Criteria

- `users` table exists after migration with correct columns/constraints.
- `UserRepository` test passes.
- Bootstrap admin row present after a fresh `docker compose up`.

## Out of Scope

- Controller/Service layer for users (`TCM-8`).
- Authentication (`TCM-7`).
