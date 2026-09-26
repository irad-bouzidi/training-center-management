package com.tcm.attendance.dto;

import com.tcm.attendance.model.AttendanceMethod;
import com.tcm.attendance.model.AttendanceStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * One row of a student's own attendance history ({@code GET
 * /api/v1/attendance/mine}): the mark plus enough of the session and its
 * course to render it without a second lookup. There is no student summary -
 * the student is the caller.
 */
public record MyAttendanceResponse(
        UUID id,
        UUID sessionId,
        UUID courseId,
        String courseCode,
        String courseName,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        AttendanceStatus status,
        AttendanceMethod method,
        Instant markedAt
) {
}
