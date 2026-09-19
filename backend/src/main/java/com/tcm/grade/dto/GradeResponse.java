package com.tcm.grade.dto;

import com.tcm.grade.model.AssessmentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GradeResponse(
        UUID id,
        UserSummary student,
        CourseSummary course,
        AssessmentType assessmentType,
        String title,
        BigDecimal score,
        BigDecimal maxScore,
        BigDecimal weight,
        /** {@code score / maxScore} as a percentage, so a reader doesn't have to divide. */
        Double percentage,
        UserSummary gradedBy,
        Instant gradedAt,
        String comments
) {
    public record UserSummary(UUID id, String name) {
    }

    public record CourseSummary(UUID id, String code, String name) {
    }
}
