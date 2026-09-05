# TCM-16 — Frontend Course Catalog & Enrollment Flow

**Branch**: `TCM-16-frontend-course-catalog-enrollment`
**Depends on**: TCM-12, TCM-14

## Goal

Let Students browse the published catalog and request enrollment; let
Admins review/approve/reject pending enrollments.

## Steps

1. `src/api/enrollmentApi.js` + hooks: `useMyEnrollmentsQuery`,
   `useRegisterMutation`, `useDecideEnrollmentMutation`,
   `useCancelEnrollmentMutation`.
2. Extend the shared catalog page (`TCM-12`) for Students: each course
   `Card` gets an "Enroll" button — disabled + labeled ("Already enrolled" /
   "Pending approval" / "Full") based on `useMyEnrollmentsQuery` + course
   capacity vs. approved count (expose `approvedCount` on
   `CourseResponse` from `TCM-11`/`TCM-14` if not already present — small
   backend addition allowed here if missed).
3. `src/features/students/MyEnrollmentsPage.jsx` (`/student/enrollments`) —
   list of the student's enrollments with status badges and a "Cancel"
   action for `PENDING`/`APPROVED` rows.
4. `src/features/enrollments/EnrollmentApprovalsPage.jsx`
   (`/admin/enrollments`, ADMIN) — table of `PENDING` (default filter) plus
   all-status view, Approve/Reject row actions with confirm dialogs,
   optimistic status badge update.
5. Add nav entries: "My Enrollments" (Student), "Enrollments" (Admin).
6. Toasts on register/cancel/approve/reject.

## Acceptance Criteria

- A Student can enroll in a published course from the catalog and see it
  as "Pending" in "My Enrollments".
- An Admin sees the pending request and can approve/reject it; the
  Student's view updates accordingly on next fetch/refetch.
- A full course shows "Full" and disables enrollment.

## Out of Scope

- Waitlisting when full (not in the brief).
