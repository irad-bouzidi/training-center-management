package com.tcm.user.dto;

import com.tcm.user.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Shared create/update payload. {@code password} is only meaningful (and
 * required) on create - see {@code UserServiceImpl.create} - update ignores
 * it (password changes go through {@code POST /users/{id}/reset-password}).
 * {@code role} must be present on every request, including a self-PUT: the
 * service layer rejects a non-admin actually changing it, rather than the
 * field being conditionally required.
 */
public record UserRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        String password,
        String phone,
        @NotNull Role role
) {
}
