package com.tcm.common;

import java.time.Instant;

/**
 * Standard error response shape, per docs/PLAN.md §6.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
