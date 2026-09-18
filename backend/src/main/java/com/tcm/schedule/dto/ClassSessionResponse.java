package com.tcm.schedule.dto;

import com.tcm.schedule.model.SessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ClassSessionResponse(
        UUID id,
        CourseSummary course,
        TrainerSummary trainer,
        String classroom,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        SessionStatus status
) {
    public record CourseSummary(UUID id, String code, String name) {
    }

    public record TrainerSummary(UUID id, String name) {
    }
}
