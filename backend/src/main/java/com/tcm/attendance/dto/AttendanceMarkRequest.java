package com.tcm.attendance.dto;

import com.tcm.attendance.model.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** One student's mark within a {@link AttendanceBulkMarkRequest}. */
public record AttendanceMarkRequest(
        @NotNull UUID studentId,
        @NotNull AttendanceStatus status
) {
}
