# Training Center Management Application — Global Plan

This document is the single source of truth for architecture, conventions, and
the domain model used across every implementation task in `docs/tasks/`. Read
this first before implementing any `TCM-*` task.

## 1. Source Requirements

See [`Training Center Management Application.md`](../Training%20Center%20Management%20Application.md)
at the repo root for the original feature brief. Summary:

- **Roles**: Administrator, Trainer, Student.
- **Core features**: Course/Training management, Student management,
  Enrollment/Registration, Course Scheduling, Attendance Management, Payment
  Management, Grades & Assessments, PDF Certificate Generation.
- **Bonus feature**: QR Code Attendance.

## 2. Tech Stack

| Layer      | Choice |
|------------|--------|
| Backend    | Java 21, Spring Boot 3.x, Spring Web, Spring Security, Spring Data JPA |
| Backend arch | Layered: **Controller → Service → Repository**, with DTOs + MapStruct (or manual mappers) between layers |
| Auth       | JWT (stateless), Spring Security filter chain, role-based `@PreAuthorize` |
| Database   | PostgreSQL 16 |
| Migrations | Liquibase (YAML changelogs), one changelog file per change, included from a master changelog |
| PDF        | OpenPDF (or iText, license-permitting) for certificate generation |
| QR Codes   | ZXing (`com.google.zxing`) for QR image generation |
| Build      | Maven (`backend/pom.xml`) |
| Frontend   | React (JavaScript, not TypeScript), Vite as bundler |
| Frontend UI| shadcn/ui (Radix + Tailwind CSS) component system |
| Frontend state/data | React Router, TanStack Query for server state, React Context for auth session |
| Containerization | Docker for every service; Docker Compose to orchestrate `postgres`, `backend`, `frontend` (and `nginx` if needed) for local/dev and prod-like runs |

## 3. Repository Layout

```
/
├── docs/
│   ├── PLAN.md                  (this file)
│   └── tasks/
│       └── TCM-<n>-<slug>.md    (one file per implementation branch/task)
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/tcm/
│       │   ├── TcmApplication.java
│       │   ├── config/                 (SecurityConfig, CorsConfig, OpenApiConfig, ...)
│       │   ├── security/                (JWT filter, JwtService, UserDetailsService impl)
│       │   ├── common/                  (base exceptions, ApiError, pagination helpers)
│       │   ├── user/                    (User domain: controller/service/repository/model/dto)
│       │   ├── course/
│       │   ├── enrollment/
│       │   ├── schedule/
│       │   ├── attendance/
│       │   ├── payment/
│       │   ├── grade/
│       │   ├── certificate/
│       │   └── qrattendance/
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── application-docker.yml
│       │   └── db/changelog/
│       │       ├── db.changelog-master.yaml
│       │       └── changes/
│       │           └── <date>-<seq>-<description>.yaml
│       └── test/java/com/tcm/...
├── frontend/
│   ├── package.json
│   ├── Dockerfile
│   ├── vite.config.js
│   ├── components.json            (shadcn config)
│   └── src/
│       ├── main.jsx
│       ├── App.jsx
│       ├── api/                   (axios client, per-domain api modules)
│       ├── components/            (shadcn ui/ + shared components)
│       ├── features/              (per-domain: courses/, students/, enrollments/, schedule/, attendance/, payments/, grades/, certificates/)
│       ├── layouts/               (AdminLayout, TrainerLayout, StudentLayout)
│       ├── routes/                (route definitions + ProtectedRoute)
│       ├── context/                (AuthContext)
│       └── lib/                   (utils, cn helper)
├── docker-compose.yml
├── docker-compose.override.yml (optional dev overrides, hot-reload)
├── .env.example
└── README.md
```

Every domain package on the backend follows the same internal shape:

```
<domain>/
├── <Domain>Controller.java
├── <Domain>Service.java
├── <Domain>ServiceImpl.java   (interface + impl, or a single Service class if simple)
├── <Domain>Repository.java
├── model/<Domain>.java         (JPA entity)
├── dto/<Domain>Request.java, <Domain>Response.java
└── mapper/<Domain>Mapper.java
```

## 4. Branching & Task Convention

- Every task file in `docs/tasks/` is named `TCM-<n>-<slug>.md` and maps 1:1 to
  a git branch named identically: `TCM-<n>-<slug>`.
- Branches are created off `main`, implemented, then merged back to `main` via
  the normal review flow before moving to the next task.
- Numbering is strictly incremental and encodes the implementation order —
  each task assumes all lower-numbered tasks are already merged.
- Each task file has the same structure: Goal, Depends On, Branch, Backend
  Steps, Frontend Steps (if any), Docker/Infra Steps (if any), Acceptance
  Criteria (Definition of Done), Out of Scope.

## 5. Domain Model (Entity Reference)

All tables use a surrogate `id BIGINT/UUID` primary key (UUID recommended for
public-facing entities), plus `created_at` / `updated_at` timestamps.

### `users`
Single table for all roles (Administrator, Trainer, Student), differentiated
by `role`.

| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| first_name | varchar | |
| last_name | varchar | |
| email | varchar | unique, login identifier |
| password_hash | varchar | BCrypt |
| phone | varchar | nullable |
| role | enum | `ADMIN`, `TRAINER`, `STUDENT` |
| status | enum | `ACTIVE`, `INACTIVE` |
| created_at / updated_at | timestamp | |

### `courses`
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| code | varchar | unique, e.g. `JAVA-101` |
| name | varchar | |
| description | text | |
| duration_hours | int | |
| capacity | int | max students |
| category | varchar | nullable |
| primary_trainer_id | UUID | FK → users(id), nullable |
| price | numeric(10,2) | |
| status | enum | `DRAFT`, `PUBLISHED`, `ARCHIVED` |
| created_at / updated_at | timestamp | |

### `enrollments`
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| student_id | UUID | FK → users(id) |
| course_id | UUID | FK → courses(id) |
| status | enum | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED` |
| enrolled_at | timestamp | |
| decided_at | timestamp | nullable |
| decided_by | UUID | FK → users(id), nullable |
| unique(student_id, course_id) | | one enrollment per student per course |

### `class_sessions` (Course Scheduling)
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| course_id | UUID | FK → courses(id) |
| trainer_id | UUID | FK → users(id) |
| classroom | varchar | |
| session_date | date | |
| start_time | time | |
| end_time | time | |
| status | enum | `SCHEDULED`, `CANCELLED`, `COMPLETED` |
| qr_token | varchar | nullable, bonus feature |
| qr_expires_at | timestamp | nullable, bonus feature |

### `attendance_records`
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| session_id | UUID | FK → class_sessions(id) |
| student_id | UUID | FK → users(id) |
| status | enum | `PRESENT`, `ABSENT`, `LATE` |
| marked_at | timestamp | |
| marked_by | UUID | FK → users(id), nullable when via QR |
| method | enum | `MANUAL`, `QR` |
| unique(session_id, student_id) | | |

### `payments`
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| student_id | UUID | FK → users(id) |
| course_id | UUID | FK → courses(id) |
| amount_due | numeric(10,2) | |
| amount_paid | numeric(10,2) | default 0 |
| status | enum | `PENDING`, `PARTIAL`, `PAID`, `OVERDUE` |
| due_date | date | |
| paid_at | timestamp | nullable |
| payment_method | varchar | nullable |
| notes | text | nullable |

### `grades`
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| student_id | UUID | FK → users(id) |
| course_id | UUID | FK → courses(id) |
| assessment_type | enum | `EXAM`, `ASSIGNMENT`, `QUIZ`, `PROJECT` |
| title | varchar | |
| score | numeric(5,2) | |
| max_score | numeric(5,2) | |
| weight | numeric(5,2) | percentage weight in final grade |
| graded_by | UUID | FK → users(id) |
| graded_at | timestamp | |
| comments | text | nullable |

### `certificates`
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| student_id | UUID | FK → users(id) |
| course_id | UUID | FK → courses(id) |
| certificate_number | varchar | unique, human-readable |
| issued_at | timestamp | |
| file_path | varchar | storage location of generated PDF |
| generated_by | UUID | FK → users(id) |

## 6. API Conventions

- Base path: `/api/v1`.
- JSON request/response bodies; DTOs only cross controller boundaries, never
  entities.
- Auth: `Authorization: Bearer <jwt>` header. `/api/v1/auth/**` is public.
- Standard error shape: `{ "timestamp", "status", "error", "message", "path" }`.
- Pagination: `?page=&size=&sort=` query params, Spring `Pageable`, responses
  wrapped as `{ content: [...], page, size, totalElements, totalPages }`.
- Role checks via `@PreAuthorize("hasRole('ADMIN')")` etc. at the controller
  method level; ownership checks (e.g. a student may only see their own
  payments) enforced in the service layer.

## 7. Ordered Task List

Phase 0 — Foundation:
1. [TCM-1-repo-scaffolding](tasks/TCM-1-repo-scaffolding.md)
2. [TCM-2-backend-bootstrap](tasks/TCM-2-backend-bootstrap.md)
3. [TCM-3-database-liquibase-setup](tasks/TCM-3-database-liquibase-setup.md)
4. [TCM-4-frontend-bootstrap](tasks/TCM-4-frontend-bootstrap.md)
5. [TCM-5-docker-compose-orchestration](tasks/TCM-5-docker-compose-orchestration.md)

Phase 1 — Auth & Users:
6. [TCM-6-user-role-domain-model](tasks/TCM-6-user-role-domain-model.md)
7. [TCM-7-authentication-jwt](tasks/TCM-7-authentication-jwt.md)
8. [TCM-8-user-management-api](tasks/TCM-8-user-management-api.md)
9. [TCM-9-frontend-auth](tasks/TCM-9-frontend-auth.md)
10. [TCM-10-frontend-user-management](tasks/TCM-10-frontend-user-management.md)

Phase 2 — Courses:
11. [TCM-11-course-management-api](tasks/TCM-11-course-management-api.md)
12. [TCM-12-frontend-course-management](tasks/TCM-12-frontend-course-management.md)

Phase 3 — Students & Enrollment:
13. [TCM-13-student-profile-api](tasks/TCM-13-student-profile-api.md)
14. [TCM-14-enrollment-api](tasks/TCM-14-enrollment-api.md)
15. [TCM-15-frontend-student-directory](tasks/TCM-15-frontend-student-directory.md)
16. [TCM-16-frontend-course-catalog-enrollment](tasks/TCM-16-frontend-course-catalog-enrollment.md)

Phase 4 — Scheduling:
17. [TCM-17-course-scheduling-api](tasks/TCM-17-course-scheduling-api.md)
18. [TCM-18-frontend-scheduling](tasks/TCM-18-frontend-scheduling.md)

Phase 5 — Attendance:
19. [TCM-19-attendance-api](tasks/TCM-19-attendance-api.md)
20. [TCM-20-frontend-attendance](tasks/TCM-20-frontend-attendance.md)

Phase 6 — Payments:
21. [TCM-21-payment-api](tasks/TCM-21-payment-api.md)
22. [TCM-22-frontend-payment](tasks/TCM-22-frontend-payment.md)

Phase 7 — Grades:
23. [TCM-23-grades-api](tasks/TCM-23-grades-api.md)
24. [TCM-24-frontend-grades](tasks/TCM-24-frontend-grades.md)

Phase 8 — Certificates:
25. [TCM-25-certificate-generation-api](tasks/TCM-25-certificate-generation-api.md)
26. [TCM-26-frontend-certificates](tasks/TCM-26-frontend-certificates.md)

Phase 9 — Bonus: QR Attendance:
27. [TCM-27-qr-attendance-api](tasks/TCM-27-qr-attendance-api.md)
28. [TCM-28-frontend-qr-attendance](tasks/TCM-28-frontend-qr-attendance.md)

Phase 10 — Reports & Polish:
29. [TCM-29-dashboard-reports](tasks/TCM-29-dashboard-reports.md)
30. [TCM-30-final-polish-seed-data](tasks/TCM-30-final-polish-seed-data.md)

## 8. Definition of Done (applies to every task unless overridden)

- Code compiles/builds (`mvn clean verify` for backend, `npm run build` for
  frontend).
- New backend endpoints have at least a service-layer unit test and a
  controller-level integration test (`@SpringBootTest` + Testcontainers or
  MockMvc as appropriate).
- New/changed tables ship as a new Liquibase changelog file, never edits to a
  previously-run changelog.
- `docker compose up` still brings up the whole stack successfully after the
  task's changes.
- README/PLAN updated if the task changes conventions or adds new env vars.
