package com.tcm.course.dto;

import com.tcm.course.model.CourseStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        String code,
        String name,
        String description,
        Integer durationHours,
        Integer capacity,
        String category,
        TrainerSummary primaryTrainer,
        BigDecimal price,
        CourseStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    /** Null when the course currently has no assigned trainer. */
    public record TrainerSummary(UUID id, String name) {
    }
}
