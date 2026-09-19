package com.tcm.dashboard.dto;

/**
 * The trainer's own figures, per docs/tasks/TCM-29 step 2 - the same shape of
 * answer as the admin's, scoped to what they teach.
 *
 * @param sessionsAwaitingAttendance sessions they have delivered with nobody
 *                                   marked at all.
 * @param studentsAwaitingGrades     students approved on their courses with
 *                                   nothing graded yet.
 */
public record TrainerDashboardResponse(
        long myCourses,
        long upcomingSessions,
        long sessionsAwaitingAttendance,
        long studentsAwaitingGrades
) {
}
