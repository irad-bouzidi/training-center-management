package com.tcm.course.dto;

import com.tcm.course.model.CourseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

public record CourseRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotNull @Positive Integer durationHours,
        @NotNull @Positive Integer capacity,
        String category,
        UUID primaryTrainerId,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotNull CourseStatus status
) {
}
