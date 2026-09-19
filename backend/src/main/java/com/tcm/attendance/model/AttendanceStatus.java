package com.tcm.attendance.model;

/**
 * How a student turned up to a session, per docs/PLAN.md §5. LATE counts as
 * attended for every rate we report - see
 * {@code AttendanceServiceImpl#attendanceRate}.
 */
public enum AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE
}
