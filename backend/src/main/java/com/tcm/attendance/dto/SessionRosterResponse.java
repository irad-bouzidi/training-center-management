package com.tcm.attendance.dto;

import com.tcm.attendance.model.AttendanceMethod;
import com.tcm.attendance.model.AttendanceStatus;
import com.tcm.schedule.dto.ClassSessionResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * What the trainer's marking screen renders: the session itself plus one row
 * per APPROVED-enrolled student. {@link Entry#status} is null for a student
 * nobody has marked yet, which is what makes "unmarked" distinguishable from
 * ABSENT.
 */
public record SessionRosterResponse(
        ClassSessionResponse session,
        List<Entry> entries
) {
    public record Entry(
            UUID studentId,
            String studentName,
            String email,
            AttendanceStatus status,
            AttendanceMethod method,
            Instant markedAt
    ) {
    }
}
