# TCM-14 — Enrollment/Registration Management API

**Branch**: `TCM-14-enrollment-api`
**Depends on**: TCM-13

## Goal

Students register for published courses; Administrators approve/reject and
track enrollments, per `docs/PLAN.md` §5 `enrollments` table.

## Steps

1. Liquibase changelog: create `enrollments` table (FKs to `users` and
   `courses`, unique `(student_id, course_id)`, check constraint on
   `status`).
2. `com.tcm.enrollment` package:
   - `model/Enrollment.java`, `model/EnrollmentStatus.java`.
   - `EnrollmentRepository` with `existsByStudentIdAndCourseId`,
     `findByStudentId`, `findByCourseId`, paginated search by status.
   - `dto/EnrollmentRequest.java` (courseId — studentId inferred from the
     authenticated principal for self-registration; admin endpoints accept
     an explicit studentId).
   - `dto/EnrollmentResponse.java` (student summary, course summary,
     status, enrolledAt, decidedAt, decidedBy).
   - `EnrollmentService`/`EnrollmentServiceImpl`:
     - `register(studentId, courseId)` — validates course is `PUBLISHED`,
       capacity not exceeded (count of `APPROVED` enrollments <
       `course.capacity`), no existing enrollment for that pair → creates
       `PENDING`.
     - `decide(enrollmentId, APPROVED|REJECTED, adminId)`.
     - `cancel(enrollmentId, requesterId)` — student can cancel their own
       `PENDING`/`APPROVED` enrollment.
     - `markCompleted(enrollmentId)` — used later by course-completion
       logic (`TCM-25` certificate flow may call this, or a scheduled/
       manual admin action).
     - `search(...)` paginated, filters by course/student/status.
   - `EnrollmentController.java`:
     - `POST /api/v1/enrollments` (STUDENT) — self-register.
     - `POST /api/v1/enrollments/{id}/decision` (ADMIN) — approve/reject.
     - `POST /api/v1/enrollments/{id}/cancel` (STUDENT, own only; ADMIN,
       any).
     - `GET /api/v1/enrollments` (ADMIN, filterable) / `GET
       /api/v1/enrollments/mine` (STUDENT) / `GET
       /api/v1/enrollments?courseId=` (TRAINER, for their own courses).
3. Update `com.tcm.user`'s `StudentSummaryResponse` (from `TCM-13`) to
   populate the real `enrollments` list for a given student.
4. Tests: capacity-exceeded rejection, duplicate-enrollment rejection,
   approve/reject flow, student cancelling own vs. others' enrollment (403).

## Acceptance Criteria

- A student can request enrollment in a published course; it appears as
  `PENDING` for admin review.
- Admin approve/reject transitions work and are reflected in
  `GET /enrollments/mine`.
- Capacity and duplicate-enrollment rules are enforced with clear 400s.
- `GET /students/{id}/summary` now shows real enrollment data.

## Out of Scope

- Frontend (`TCM-16`).
- Attendance/grades/payments tied to enrollment (later tasks; they
  reference `student_id`/`course_id` directly, not `enrollment_id`, per the
  schema in `docs/PLAN.md`).
