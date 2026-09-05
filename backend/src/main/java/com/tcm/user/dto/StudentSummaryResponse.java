package com.tcm.user.dto;

import com.tcm.enrollment.dto.EnrollmentResponse;
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
 * {@code enrollments} is populated for real as of TCM-14; the remaining
 * lists/aggregates are still stubs populated by a later task - see the
 * {@code // TODO} markers on {@link com.tcm.user.mapper.UserMapper#toSummaryResponse}.
 */
public record StudentSummaryResponse(
        UserResponse profile,
        List<EnrollmentResponse> enrollments,
        Double attendanceRate,
        List<Object> grades,
        BigDecimal paymentBalance,
        List<Object> certificates
) {
}
