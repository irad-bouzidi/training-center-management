# TCM-17 — Course Scheduling API

**Branch**: `TCM-17-course-scheduling-api`
**Depends on**: TCM-11

## Goal

Backend for `class_sessions` — the concrete dated/timed sessions of a
course, each with a classroom and trainer, per `docs/PLAN.md` §5.

## Steps

1. Liquibase changelog: create `class_sessions` (FKs to `courses`,
   `users` for `trainer_id`; `qr_token`/`qr_expires_at` columns included now
   —nullable— even though populated only from `TCM-27`, to avoid a later
   migration just for two columns).
2. `com.tcm.schedule` package:
   - `model/ClassSession.java`, `model/SessionStatus.java`.
   - `ClassSessionRepository` — find by course, by trainer, by date range,
     with overlap-detection query (same trainer or same classroom,
     overlapping date+time range).
   - `dto/ClassSessionRequest.java` (courseId, trainerId, classroom,
     sessionDate, startTime, endTime).
   - `dto/ClassSessionResponse.java` (course summary, trainer summary,
     classroom, date/time, status).
   - `ClassSessionService`/`Impl`:
     - `create` — validates `endTime > startTime`, validates the trainer
       has role `TRAINER`, and rejects overlapping sessions for the same
       trainer or classroom (409 Conflict with a descriptive message).
     - `update`, `cancel`, `markCompleted`.
     - `search` — by course, trainer, date range (used for calendar views).
   - `ClassSessionController.java`:
     - `POST /api/v1/sessions` (ADMIN).
     - `PUT /api/v1/sessions/{id}` (ADMIN).
     - `PATCH /api/v1/sessions/{id}/status` (ADMIN, or the assigned
       TRAINER for `COMPLETED`).
     - `GET /api/v1/sessions?courseId=&trainerId=&from=&to=` (ADMIN, and
       TRAINER/STUDENT scoped to their own — trainer sees their sessions,
       student sees sessions for courses they're `APPROVED` in).
3. Tests: overlap detection (trainer double-booked, classroom double-
   booked), role-scoped visibility.

## Acceptance Criteria

- Admin can schedule sessions for a course with a trainer and classroom;
  overlapping bookings are rejected.
- Trainers see only their own sessions; Students see only sessions for
  courses they're approved/enrolled in.

## Out of Scope

- Attendance marking (`TCM-19`).
- QR token generation/rotation logic (`TCM-27` — columns exist, logic
  doesn't yet).
- Frontend (`TCM-18`).
