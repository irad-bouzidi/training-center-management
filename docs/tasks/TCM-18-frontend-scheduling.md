# TCM-18 — Frontend Course Scheduling

**Branch**: `TCM-18-frontend-scheduling`
**Depends on**: TCM-12, TCM-17

## Goal

Calendar/list views of class sessions for all three roles, and the
admin scheduling form.

## Steps

1. `src/api/scheduleApi.js` + hooks (`useSessionsQuery`,
   `useCreateSessionMutation`, etc.).
2. `src/features/schedule/ScheduleFormDialog.jsx` (ADMIN) — course select,
   trainer select (filtered to `TRAINER` role, optionally pre-filled from
   course's primary trainer), classroom input, date + start/end time
   pickers (shadcn `Calendar` + `Popover` for date, plain time inputs),
   surfaces the 409 overlap error inline.
3. `src/features/schedule/ScheduleListPage.jsx` — shared component
   parameterized by role:
   - Admin (`/admin/schedule`): all sessions, filters (course, trainer,
     date range), "New Session" action.
   - Trainer (`/trainer/schedule`): own sessions only, read-only + a
     "Mark Completed" action.
   - Student (`/student/schedule`): sessions for their approved courses,
     read-only.
   Render as a simple week/list agenda view (a full calendar grid library
   is optional/nice-to-have, not required — a grouped-by-date list is
   sufficient to satisfy the brief).
4. Add "Schedule" nav entry to all three layouts; enable the "Schedule" tab
   stub added on the course detail page (`TCM-12`) and student summary page
   (`TCM-15`).

## Acceptance Criteria

- Admin can create a session and see the double-booking error surfaced
  clearly when applicable.
- Each role sees the correctly scoped set of sessions in their schedule
  view.

## Out of Scope

- Attendance marking UI (`TCM-20`).
- QR display (`TCM-28`).
