# TCM-24 — Frontend Grades & Assessments

**Branch**: `TCM-24-frontend-grades`
**Depends on**: TCM-15, TCM-23

## Goal

Trainer gradebook UI; student grade view. Enables the "Grades" tab stub
from `TCM-15`.

## Steps

1. `src/api/gradeApi.js` + hooks (`useCourseGradebookQuery`,
   `useCreateGradeMutation`, `useUpdateGradeMutation`,
   `useStudentGradesQuery`).
2. `src/features/grades/GradebookPage.jsx`
   (`/trainer/courses/:courseId/grades`) — table of students × assessment
   entries (expandable rows or a per-student drill-in), "Add Assessment"
   dialog (type select, title, score/maxScore, weight, comments), inline
   edit/delete on existing entries.
3. Populate the "Grades" tab on `StudentSummaryPage` (`TCM-15`) with the
   student's per-course assessment list and computed overall score
   (progress bar or badge).
4. `src/features/grades/MyGradesPage.jsx` (`/student/grades`) — student's
   own grades across all enrolled courses, grouped by course, with the
   computed overall performance per course.
5. Nav: "Gradebook" link from a Trainer's course detail; "My Grades" nav
   item for Students.

## Acceptance Criteria

- Trainer can add/edit/delete assessment entries for their course's
  students.
- Student sees an accurate, up-to-date view of their own grades and
  overall performance per course.

## Out of Scope

- Grade-based certificate eligibility UI (`TCM-26`).
