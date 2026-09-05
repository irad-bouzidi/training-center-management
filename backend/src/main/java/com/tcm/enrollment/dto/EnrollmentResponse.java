package com.tcm.enrollment.dto;

import com.tcm.enrollment.model.EnrollmentStatus;
import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponse(
        UUID id,
        UserSummary student,
        CourseSummary course,
        EnrollmentStatus status,
        Instant enrolledAt,
        Instant decidedAt,
        UserSummary decidedBy
) {
    /** Reused for both {@code student} and {@code decidedBy} - both are plain users. */
    public record UserSummary(UUID id, String name, String email) {
    }

    public record CourseSummary(UUID id, String code, String name) {
    }
}
