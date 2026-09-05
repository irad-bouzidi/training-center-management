package com.tcm.enrollment.dto;

import com.tcm.enrollment.model.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

/** {@code POST /api/v1/enrollments/{id}/decision}. {@code status} must be APPROVED or REJECTED. */
public record EnrollmentDecisionRequest(
        @NotNull EnrollmentStatus status
) {
}
