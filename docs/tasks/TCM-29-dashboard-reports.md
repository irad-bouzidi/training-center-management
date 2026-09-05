# TCM-29 — Admin Dashboard & Cross-Cutting Reports

**Branch**: `TCM-29-dashboard-reports`
**Depends on**: TCM-21, TCM-23, TCM-19, TCM-14

## Goal

A top-level Administrator dashboard aggregating the platform's key metrics,
plus a couple of cross-cutting report endpoints not covered by any single
domain (the per-domain reports already exist: attendance report `TCM-19`,
gradebook `TCM-23`, payments list `TCM-21`).

## Steps

1. `com.tcm.dashboard` package (read-only aggregation, no new tables):
   - `DashboardService` — pulls counts via existing repositories:
     total active students/trainers, total published courses, pending
     enrollments count, upcoming sessions (next 7 days), outstanding
     payment balance sum, overdue invoices count, average attendance rate
     across all courses, certificates issued this month.
   - `DashboardController.GET /api/v1/dashboard/summary` (ADMIN).
2. Add a lightweight Trainer dashboard variant:
   `GET /api/v1/dashboard/trainer-summary` (TRAINER) — their course count,
   upcoming sessions, pending gradebook items (sessions completed but no
   grades yet — best-effort heuristic, not a hard requirement).
3. Frontend:
   - `src/features/dashboard/AdminDashboardPage.jsx` (`/admin`, the Admin
     home route wired back in `TCM-9`) — shadcn `Card` grid of stat tiles
     (use the `dataviz` skill for any chart/number-tile styling
     guidance), quick links into each admin section.
   - `src/features/dashboard/TrainerDashboardPage.jsx` (`/trainer`) —
     similar tile grid scoped to the trainer.
   - Student home (`/student`) can simply surface their own summary
     (enrollments/attendance/grades/payments/certificates) already built by
     prior tasks — compose from `StudentSummaryPage`'s data rather than a
     new endpoint.
4. Tests: dashboard aggregation numbers match manually-seeded expected
   values in a Testcontainers-backed integration test.

## Acceptance Criteria

- Admin home page shows accurate live counts across all domains.
- Trainer home page shows their own scoped counts.
- No new tables introduced — this task is pure aggregation over existing
  data.

## Out of Scope

- Data visualization charts beyond simple stat tiles (nice-to-have only if
  time allows; a numeric tile grid satisfies the brief's "reports"
  requirement).
