# TCM-20 — Frontend Attendance Management

**Branch**: `TCM-20-frontend-attendance`
**Depends on**: TCM-18, TCM-19

## Goal

Trainer UI to mark session attendance; Admin UI for attendance reports;
enable the "Attendance" tab stubs from `TCM-15`/`TCM-12`.

## Steps

1. `src/api/attendanceApi.js` + hooks (`useSessionRosterQuery`,
   `useMarkAttendanceMutation`, `useCourseAttendanceReportQuery`).
2. `src/features/attendance/MarkAttendancePage.jsx`
   (`/trainer/sessions/:sessionId/attendance`) — roster table with a
   segmented control per row (Present/Absent/Late, shadcn `ToggleGroup` or
   `RadioGroup`), a "Mark all present" bulk helper, single "Save" that
   bulk-submits. Reachable via a "Take Attendance" button on the Trainer's
   schedule list (`TCM-18`).
3. `src/features/attendance/AttendanceReportPage.jsx`
   (`/admin/courses/:courseId/attendance`, enables the course detail
   "Attendance" tab) — per-student table with present/absent/late counts
   and a percentage bar/badge.
4. Populate the "Attendance" tab on `StudentSummaryPage` (`TCM-15`) with the
   student's per-course attendance breakdown.

## Acceptance Criteria

- Trainer can open a session and mark the full roster in one save action.
- Admin sees an accurate attendance report per course.
- Student summary page shows real attendance data instead of "coming
  soon".

## Out of Scope

- QR scan UI (`TCM-28`).
