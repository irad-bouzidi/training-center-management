package com.tcm.attendance.dto;

import java.util.List;
import java.util.UUID;

/**
 * Admin-facing roll-up for one course: every APPROVED-enrolled student with
 * their tallies across the course's sessions. {@code attendanceRate} is the
 * percentage of that student's *marked* sessions at which they turned up
 * (PRESENT or LATE), and is null while they have no marks at all - an
 * unmarked student is not a 0% student.
 */
public record CourseAttendanceReportResponse(
        UUID courseId,
        String courseCode,
        String courseName,
        int sessionCount,
        List<StudentRow> students
) {
    public record StudentRow(
            UUID studentId,
            String studentName,
            String email,
            long present,
            long absent,
            long late,
            long marked,
            Double attendanceRate
    ) {
    }
}
