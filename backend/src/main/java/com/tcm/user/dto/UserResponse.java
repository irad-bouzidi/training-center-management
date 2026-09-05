package com.tcm.user.dto;

import com.tcm.user.model.Role;
import com.tcm.user.model.UserStatus;
import java.time.Instant;
import java.util.UUID;

/** Never carries {@code passwordHash} - see docs/tasks/TCM-8. */
public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        UserStatus status,
        Instant createdAt
) {
}
