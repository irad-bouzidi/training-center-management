package com.tcm.user.dto;

import com.tcm.user.model.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull UserStatus status
) {
}
