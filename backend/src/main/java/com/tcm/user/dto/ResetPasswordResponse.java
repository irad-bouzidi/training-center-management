package com.tcm.user.dto;

/**
 * The new temporary password, returned once so the Administrator can relay
 * it to the user out of band - there's no email delivery system yet.
 */
public record ResetPasswordResponse(
        String tempPassword
) {
}
