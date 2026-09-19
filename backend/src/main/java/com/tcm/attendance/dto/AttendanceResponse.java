package com.tcm.attendance.dto;

import com.tcm.attendance.model.AttendanceMethod;
import com.tcm.attendance.model.AttendanceStatus;
import java.time.Instant;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        UUID sessionId,
        StudentSummary student,
        AttendanceStatus status,
        Instant markedAt,
        AttendanceMethod method
) {
    public record StudentSummary(UUID id, String name, String email) {
    }
}
