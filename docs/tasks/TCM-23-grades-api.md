# TCM-23 — Grades & Assessments API

**Branch**: `TCM-23-grades-api`
**Depends on**: TCM-14

## Goal

Backend for trainers to record exam/assignment/quiz/project results and
students to view them, per `docs/PLAN.md` §5 `grades`.

## Steps

1. Liquibase changelog: create `grades` (FKs to `users` for `student_id`
   and `graded_by`, `courses`; check constraint on `assessment_type`;
   `score <= max_score` check).
2. `com.tcm.grade` package:
   - `model/Grade.java`, `model/AssessmentType.java`.
   - `GradeRepository` — find by student, by course, by student+course.
   - `dto/GradeRequest.java` (studentId, courseId, assessmentType, title,
     score, maxScore, weight, comments) — validated: student must have an
     `APPROVED` enrollment in the course; `graderId` (trainer) must be the
     course's assigned trainer or an Admin.
   - `dto/GradeResponse.java`.
   - `GradeService`/`Impl`:
     - `create`, `update`, `delete` (only by the original grader or Admin).
     - `findForStudentInCourse(studentId, courseId)` — list + computed
       weighted final score (`Σ(score/maxScore * weight) / Σweight`).
     - `courseGradebook(courseId)` — all students' entries, for the
       Trainer's gradebook view.
   - `GradeController.java`:
     - `POST /api/v1/grades` (assigned TRAINER, ADMIN).
     - `PUT /api/v1/grades/{id}` / `DELETE /api/v1/grades/{id}` (grader or
       ADMIN).
     - `GET /api/v1/courses/{courseId}/grades` (assigned TRAINER, ADMIN) —
       gradebook.
     - `GET /api/v1/students/{studentId}/grades?courseId=` (self, assigned
       TRAINER, ADMIN).
3. Update `StudentSummaryResponse` (`TCM-13`) with real `grades` +
   an overall performance figure.
4. Tests: weighted average computation, authorization (wrong trainer can't
   grade another trainer's course), enrollment-required validation.

## Acceptance Criteria

- Trainer can record and edit grades for students enrolled in their course.
- Student can view their own grades and a computed overall performance.
- Cross-trainer grading attempts are rejected (403).

## Out of Scope

- PDF certificate eligibility logic based on grades (`TCM-25` decides
  completion criteria; it may read from this API but does not change it).
- Frontend (`TCM-24`).
