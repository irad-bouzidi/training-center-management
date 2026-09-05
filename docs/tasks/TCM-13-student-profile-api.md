# TCM-13 — Student Profile & Enrollment History API

**Branch**: `TCM-13-student-profile-api`
**Depends on**: TCM-11

## Goal

Add the student-facing aggregation endpoints: a student's own dashboard
data and an admin-facing "student directory" view, without yet
implementing the enrollment/attendance/grade/payment tables that back it
(those land in the next tasks) — this task focuses on the read model and
wiring for the parts already available (profile), plus stub aggregation
that the following tasks will flesh out.

## Steps

1. In `com.tcm.user`, add `GET /api/v1/students` (ADMIN, TRAINER) — same as
   `GET /api/v1/users?role=STUDENT` but as a dedicated, purpose-named
   endpoint (`StudentDirectoryController` or a query param convenience on
   the existing `UserController`) returning `UserResponse` plus placeholder
   counts (`activeEnrollments: 0` etc. for now) — this keeps the frontend
   contract stable so `TCM-15` doesn't need to change once later tasks add
   real data.
2. Add `GET /api/v1/students/{id}/summary` (ADMIN, TRAINER, or self) that
   will progressively aggregate: enrollments (`TCM-14`), attendance
   (`TCM-19`), grades (`TCM-23`), payments (`TCM-21`), certificates
   (`TCM-25`). For this task, implement it returning just the profile plus
   empty arrays/zeroed stats for the not-yet-built domains, with clear
   `// TODO(TCM-14/19/21/23/25)` markers — each later task updates this one
   method to populate its slice instead of inventing a new endpoint.
3. Document the `StudentSummaryResponse` DTO shape fully up front (even
   though fields are empty initially) so later tasks only fill data in, not
   change the contract:
   ```
   { profile, enrollments: [], attendanceRate: null, grades: [],
     paymentBalance: null, certificates: [] }
   ```

## Acceptance Criteria

- `GET /api/v1/students` returns paginated students.
- `GET /api/v1/students/{id}/summary` returns the full documented shape
  with placeholder empty/null values, callable by Admin, the assigned
  Trainer, or the student themselves.

## Out of Scope

- Real enrollment/attendance/grade/payment/certificate data (filled in by
  `TCM-14`, `TCM-19`, `TCM-21`, `TCM-23`, `TCM-25` respectively — each of
  those tasks must include a step updating this summary endpoint).
