# TCM-30 — Final Polish, Seed Data & Full-Stack Verification

**Branch**: `TCM-30-final-polish-seed-data`
**Depends on**: all previous tasks (TCM-1 … TCM-29)

## Goal

Close out the project: realistic seed/demo data, end-to-end smoke
verification of the whole Dockerized stack, README finalization, and a
pass over rough edges left by earlier tasks.

## Steps

1. Add a Liquibase seed-data changelog (clearly separated from schema
   changelogs, e.g. `db/changelog/changes/<date>-seed-demo-data.yaml`,
   included only in a `dev`/`demo` context — use Liquibase contexts so it
   never runs against a `prod` profile): a handful of Trainers, Students,
   Courses (mixed statuses), Enrollments (mixed statuses), sessions,
   attendance, payments, and grades so the app is immediately explorable
   after `docker compose up`.
2. Full manual/scripted smoke pass through every role's core journey:
   - Admin: create user → create course → approve enrollment → schedule
     session → view attendance report → record payment → view dashboard.
   - Trainer: view schedule → take attendance (manual + QR) → enter grades
     → generate certificate.
   - Student: browse catalog → enroll → view schedule → check attendance/
     grades/payments → scan QR → download certificate.
   Consider using the `run` skill to actually launch and click through the
   app rather than only reading code.
3. Fix any inconsistencies surfaced by the smoke pass (cross-reference back
   to the specific `TCM-*` task that owns the broken piece rather than
   patching blindly).
4. Finalize root `README.md`: architecture diagram (simple ASCII or a
   Mermaid diagram is fine), full env var table, "Default accounts" section
   listing seeded demo logins per role, troubleshooting section.
5. Confirm `docker compose up --build` from a totally clean checkout
   (`docker compose down -v`, remove images) works with zero manual steps
   beyond `cp .env.example .env`.
6. Tag this as the v1.0 milestone (git tag, optional, only if the user asks
   for a release tag).

## Acceptance Criteria

- Fresh `docker compose up --build` yields a fully seeded, fully
  functional application covering every feature in the original brief plus
  the QR bonus feature.
- Every role's core journey (listed above) works end-to-end without
  errors.
- README accurately documents setup, env vars, and default demo accounts.

## Out of Scope

- New features beyond the original brief.
- Production deployment (cloud hosting, TLS, managed Postgres) — this task
  covers local/dev-parity Docker Compose only, consistent with `TCM-5`.
