package com.tcm.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * A whole session roster in one request - what the trainer's marking screen
 * submits. Students left out of {@code entries} keep whatever mark they
 * already had (or stay unmarked); nothing here deletes a mark.
 */
public record AttendanceBulkMarkRequest(
        @NotEmpty @Valid List<AttendanceMarkRequest> entries
) {
}
