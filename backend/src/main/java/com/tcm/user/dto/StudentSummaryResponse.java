package com.tcm.user.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Shape for {@code GET /api/v1/students/{id}/summary}, per docs/tasks/TCM-13.
 * Documented fully up front so later tasks only fill data in, never change
 * the contract:
 *
 * <pre>
 * { profile, enrollments: [], attendanceRate: null, grades: [],
 *   paymentBalance: null, certificates: [] }
 * </pre>
 *
 * Each list/aggregate below is a stub populated by a later task - see the
 * {@code // TODO} markers on {@link com.tcm.user.mapper.UserMapper#toSummaryResponse}.
 */
public record StudentSummaryResponse(
        UserResponse profile,
        List<Object> enrollments,
        Double attendanceRate,
        List<Object> grades,
        BigDecimal paymentBalance,
        List<Object> certificates
) {
}
