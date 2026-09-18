package com.tcm.schedule.dto;

import com.tcm.schedule.model.SessionStatus;
import jakarta.validation.constraints.NotNull;

/** {@code PATCH /api/v1/sessions/{id}/status}. {@code status} must be
 * CANCELLED or COMPLETED - a session is never moved back to SCHEDULED. */
public record ClassSessionStatusRequest(
        @NotNull SessionStatus status
) {
}
