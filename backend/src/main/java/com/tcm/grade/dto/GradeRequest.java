package com.tcm.grade.dto;

import com.tcm.grade.model.AssessmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Records or amends one assessment result. {@code score <= maxScore} and the
 * student's APPROVED enrollment are checked in the service, which has the
 * course to check them against; the bounds that stand on their own are
 * checked here.
 *
 * On an update, {@code studentId} and {@code courseId} are ignored - a grade
 * stays attached to the pair it was recorded for (see
 * {@code GradeServiceImpl#update}).
 */
public record GradeRequest(
        @NotNull UUID studentId,
        @NotNull UUID courseId,
        @NotNull AssessmentType assessmentType,
        @NotBlank @Size(max = 200) String title,
        @NotNull @DecimalMin(value = "0.00", message = "must not be negative") BigDecimal score,
        @NotNull @DecimalMin(value = "0.01", message = "must be greater than zero") BigDecimal maxScore,
        @NotNull @DecimalMin(value = "0.01", message = "must be greater than zero") BigDecimal weight,
        String comments
) {
}
