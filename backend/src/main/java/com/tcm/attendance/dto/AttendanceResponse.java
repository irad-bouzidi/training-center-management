package com.tcm.attendance.dto;

import com.tcm.attendance.model.AttendanceMethod;
import com.tcm.attendance.model.AttendanceStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One attendance mark. {@code courseId}/{@code courseName}/{@code sessionDate}
 * are denormalised from the session so a caller holding only this - the QR
 * check-in screen - can say what the student was marked present for.
 * Nullable in the contract, though the mapper always has them to fill.
 */
public record AttendanceResponse(
        UUID id,
        UUID sessionId,
        StudentSummary student,
        AttendanceStatus status,
        Instant markedAt,
        AttendanceMethod method,
        UUID courseId,
        String courseName,
        LocalDate sessionDate
) {
    public record StudentSummary(UUID id, String name, String email) {
    }
}
