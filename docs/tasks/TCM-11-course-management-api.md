# TCM-11 — Course/Training Program Management API

**Branch**: `TCM-11-course-management-api`
**Depends on**: TCM-8

## Goal

Backend CRUD for training programs/courses, including assigning a primary
trainer, per `docs/PLAN.md` §5 `courses` table.

## Steps

1. Liquibase changelog: create `courses` table (FK `primary_trainer_id` →
   `users(id)`, `ON DELETE SET NULL`), unique index on `code`, check
   constraint `status IN ('DRAFT','PUBLISHED','ARCHIVED')`.
2. `com.tcm.course` package:
   - `model/Course.java`, `model/CourseStatus.java` enum.
   - `CourseRepository.java extends JpaRepository<Course, UUID>` +
     `JpaSpecificationExecutor<Course>` for filterable search (by status,
     category, trainer, free-text on name/code).
   - `dto/CourseRequest.java` (code, name, description, durationHours,
     capacity, category, primaryTrainerId, price, status) with validation
     (capacity > 0, durationHours > 0, price >= 0).
   - `dto/CourseResponse.java` (adds trainer summary: id/name).
   - `mapper/CourseMapper.java`.
   - `CourseService`/`CourseServiceImpl`: `create`, `update`, `changeStatus`,
     `findById`, `search` (paginated), `delete` (only allowed if no
     enrollments exist yet — enforce once `TCM-14` lands; for now just
     delete, revisit if needed).
   - `CourseController.java`:
     - `POST /api/v1/courses` (ADMIN).
     - `GET /api/v1/courses` (any authenticated role; Students/Trainers see
       only `PUBLISHED` by default unless ADMIN, or use a
       `status` query param restricted to ADMIN).
     - `GET /api/v1/courses/{id}` (any authenticated role).
     - `PUT /api/v1/courses/{id}` (ADMIN).
     - `PATCH /api/v1/courses/{id}/status` (ADMIN).
     - `GET /api/v1/courses/mine` (TRAINER) — courses where caller is the
       primary trainer.
   - Validate `primaryTrainerId` (if provided) references a user with role
     `TRAINER`.
3. Tests: service unit tests for validation rules (invalid trainer role →
   400), controller IT for role-based visibility of `DRAFT` vs
   `PUBLISHED` courses.

## Acceptance Criteria

- Admin can create/update/list/publish/archive courses.
- Assigning a non-trainer user as `primaryTrainerId` is rejected with 400.
- Students/Trainers only ever see `PUBLISHED` courses through the general
  list endpoint; Trainers additionally see their own via `/mine` regardless
  of status.

## Out of Scope

- Course scheduling/sessions (`TCM-17`).
- Enrollment (`TCM-14`).
- Frontend (`TCM-12`).
