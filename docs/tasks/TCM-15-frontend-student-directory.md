# TCM-15 — Frontend Student Directory & Profile

**Branch**: `TCM-15-frontend-student-directory`
**Depends on**: TCM-12, TCM-14

## Goal

Admin/Trainer-facing student directory and a per-student profile/summary
page consuming `GET /students` and `GET /students/{id}/summary`.

## Steps

1. `src/api/studentApi.js`: `listStudents(params)`, `getStudentSummary(id)`.
2. `src/features/students/StudentsListPage.jsx` (`/admin/students`, also
   reachable from `TrainerLayout` as read-only) — table with name, email,
   enrollment count, status, search/filter, row click → detail.
3. `src/features/students/StudentSummaryPage.jsx` — tabs (shadcn `Tabs`):
   "Overview" (profile + quick stats), "Enrollments" (list with status
   badges, from real data now that `TCM-14` shipped), and placeholder
   disabled tabs for "Attendance", "Grades", "Payments", "Certificates"
   that later tasks (`TCM-20`, `TCM-24`, `TCM-22`, `TCM-26`) enable and
   populate.
4. Add "Students" nav entry to `AdminLayout` and `TrainerLayout`.

## Acceptance Criteria

- Admin/Trainer can browse students and open a summary page showing real
  profile + enrollment data.
- Not-yet-implemented tabs are visibly present but disabled/marked "coming
  soon", not broken links.

## Out of Scope

- Student's own self-view of this data (that's the Student role's own
  dashboard, covered incidentally by `TCM-16` catalog + later per-feature
  student pages, not a separate task).
