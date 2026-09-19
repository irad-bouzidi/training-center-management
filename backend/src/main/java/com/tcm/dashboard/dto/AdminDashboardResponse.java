package com.tcm.dashboard.dto;

import java.math.BigDecimal;

/**
 * The administrator's at-a-glance figures, per docs/tasks/TCM-29. Every one
 * is a live count over existing data - the dashboard stores nothing.
 *
 * @param averageAttendanceRate percentage of all marked sessions attended,
 *                              null while nothing has been marked anywhere -
 *                              the same "unmarked is not zero" rule the rest
 *                              of the app follows.
 */
public record AdminDashboardResponse(
        long activeStudents,
        long activeTrainers,
        long publishedCourses,
        long pendingEnrollments,
        long upcomingSessions,
        BigDecimal outstandingBalance,
        long overdueInvoices,
        Double averageAttendanceRate,
        long certificatesThisMonth
) {
}
