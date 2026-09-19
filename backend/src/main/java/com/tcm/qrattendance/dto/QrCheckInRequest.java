package com.tcm.qrattendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** What a student's phone posts after scanning the code. */
public record QrCheckInRequest(
        @NotNull UUID sessionId,
        @NotBlank String token
) {
}
