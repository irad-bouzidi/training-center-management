package com.tcm.auth.dto;

import com.tcm.user.model.Role;
import java.time.Instant;
import java.util.UUID;

public record AuthResponse(
        String token,
        Instant expiresAt,
        UserSummary user
) {
    public record UserSummary(
            UUID id,
            String name,
            String email,
            Role role
    ) {
    }
}
