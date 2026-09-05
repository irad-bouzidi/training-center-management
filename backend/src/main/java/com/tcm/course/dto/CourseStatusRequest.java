package com.tcm.course.dto;

import com.tcm.course.model.CourseStatus;
import jakarta.validation.constraints.NotNull;

public record CourseStatusRequest(
        @NotNull CourseStatus status
) {
}
