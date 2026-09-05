# TCM-19 — Attendance Management API

**Branch**: `TCM-19-attendance-api`
**Depends on**: TCM-17, TCM-14

## Goal

Backend for recording and reporting attendance per session, per
`docs/PLAN.md` §5 `attendance_records`.

## Steps

1. Liquibase changelog: create `attendance_records` (FKs to
   `class_sessions`, `users`; unique `(session_id, student_id)`; check
   constraint on `status` and `method`).
2. `com.tcm.attendance` package:
   - `model/AttendanceRecord.java`, `model/AttendanceStatus.java`
     (`PRESENT`, `ABSENT`, `LATE`), `model/AttendanceMethod.java`
     (`MANUAL`, `QR`).
   - `AttendanceRepository` — find by session, by student, aggregate
     counts per student/course for reporting.
   - `dto/AttendanceMarkRequest.java` (studentId, status) and a bulk
     variant `AttendanceBulkMarkRequest.java` (list of studentId+status for
     a whole session roster).
   - `dto/AttendanceResponse.java`.
   - `AttendanceService`/`Impl`:
     - `getRoster(sessionId)` — returns every `APPROVED`-enrolled student
       for the session's course with their current attendance status (or
       `null` if unmarked) — this is what the Trainer's marking UI renders.
     - `markOne(sessionId, studentId, status, markerId)`,
       `markBulk(sessionId, entries, markerId)` — method always `MANUAL`
       here (`QR` is set only by the `TCM-27` scan endpoint).
     - `courseAttendanceReport(courseId)` — per-student present/absent/late
       counts and percentage, for Admin reporting.
     - `studentAttendanceSummary(studentId)` — overall attendance rate
       across all their courses.
   - `AttendanceController.java`:
     - `GET /api/v1/sessions/{sessionId}/attendance` (ADMIN, assigned
       TRAINER) — roster + status.
     - `POST /api/v1/sessions/{sessionId}/attendance` (assigned TRAINER,
       ADMIN) — bulk mark.
     - `GET /api/v1/courses/{courseId}/attendance-report` (ADMIN, assigned
       TRAINER).
3. Update `StudentSummaryResponse` (`TCM-13`) to populate real
   `attendanceRate`.
4. Tests: only the assigned trainer (or admin) can mark a session's
   attendance (403 otherwise); duplicate marks update rather than duplicate
   rows (upsert semantics); report aggregation correctness.

## Acceptance Criteria

- Trainer can fetch a session's roster and bulk-mark present/absent/late.
- Admin can pull a course-level attendance report.
- `GET /students/{id}/summary` shows a real attendance rate.

## Out of Scope

- QR-based self check-in (`TCM-27`, which reuses `markOne` internally with
  `method=QR`).
- Frontend (`TCM-20`).
