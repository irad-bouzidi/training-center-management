# TCM-12 — Frontend Course Management

**Branch**: `TCM-12-frontend-course-management`
**Depends on**: TCM-10, TCM-11

## Goal

Admin UI to manage courses, and a read-only course catalog view usable by
Trainers/Students (full enroll action comes in `TCM-16`).

## Steps

1. `src/api/courseApi.js` + `src/features/courses/hooks.js`
   (`useCoursesQuery`, `useCreateCourseMutation`, etc.).
2. `src/features/courses/CoursesListPage.jsx` (Admin, `/admin/courses`) —
   shadcn `Table`: code, name, trainer, capacity, price, status badge,
   filters (status, category, search), "New Course" → `Dialog` form.
3. `src/features/courses/CourseFormDialog.jsx` — form fields per
   `CourseRequest`, trainer picked from a `Select` populated via
   `GET /users?role=TRAINER`.
4. `src/features/courses/CourseDetailPage.jsx` — full detail view (used by
   Admin and, read-only, by Trainer/Student), tabs placeholder for
   "Schedule", "Enrollments" etc. that later tasks fill in.
5. Add "Courses" nav to `AdminLayout`; add a read-only "Catalog" nav entry to
   `TrainerLayout`/`StudentLayout` pointing at `/courses` (shared catalog
   page, published-only, card grid using shadcn `Card`).
6. Status change actions (Publish/Archive) with confirm dialogs.

## Acceptance Criteria

- Admin can create a course, assign a trainer, publish it, and see it
  appear in the Student/Trainer catalog view.
- Draft courses are invisible in the shared catalog but visible in the
  admin list.

## Out of Scope

- Enrollment action button (wired in `TCM-16`).
- Scheduling tab content (`TCM-18`).
