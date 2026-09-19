package com.tcm.qrattendance.dto;

import java.time.Instant;

/**
 * A session's current QR code. The PNG comes back base64-encoded in the same
 * response as its expiry, so the screen showing it can count down without a
 * second call.
 *
 * @param checkInUrl what the image encodes - handy for a "can't scan?" link.
 */
public record QrCodeResponse(
        String token,
        Instant expiresAt,
        String checkInUrl,
        String imageBase64
) {
}
