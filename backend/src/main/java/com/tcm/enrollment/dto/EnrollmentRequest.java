package com.tcm.enrollment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * {@code POST /api/v1/enrollments}. For a STUDENT caller, {@code studentId}
 * is ignored - the enrollment is always created for the authenticated
 * principal. For an ADMIN caller registering a student on their behalf,
 * {@code studentId} is required.
 */
public record EnrollmentRequest(
        @NotNull UUID courseId,
        UUID studentId
) {
}
