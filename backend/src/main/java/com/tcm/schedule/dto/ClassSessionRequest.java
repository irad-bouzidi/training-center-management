package com.tcm.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * {@code POST /api/v1/sessions} and {@code PUT /api/v1/sessions/{id}}.
 * {@code endTime} must be strictly after {@code startTime} - checked in the
 * service rather than here, since a cross-field constraint has no useful
 * single-field annotation and the message belongs with the other scheduling
 * rules.
 */
public record ClassSessionRequest(
        @NotNull UUID courseId,
        @NotNull UUID trainerId,
        @NotBlank String classroom,
        @NotNull LocalDate sessionDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {
}
