package com.tcm.user.dto;

import com.tcm.certificate.dto.CertificateResponse;
import com.tcm.enrollment.dto.EnrollmentResponse;
import com.tcm.grade.dto.GradeResponse;
import java.math.BigDecimal;
import java.util.List;

/**
 * Shape for {@code GET /api/v1/students/{id}/summary}, per docs/tasks/TCM-13.
 * Documented fully up front so later tasks only fill data in, never change
 * the contract:
 *
 * <pre>
 * { profile, enrollments: [], attendanceRate, grades: [], overallGrade,
 *   paymentBalance, certificates: [] }
 * </pre>
 *
 * Real as of: {@code enrollments} TCM-14, {@code attendanceRate} TCM-19,
 * {@code paymentBalance} TCM-21, {@code grades}/{@code overallGrade} TCM-23,
 * {@code certificates} TCM-25 - every field now carries real data.
 *
 * {@code overallGrade} joined the shape in TCM-23 rather than being reserved
 * from the start: TCM-13 had no way to know a weighted average was the
 * figure worth carrying.
 */
public record StudentSummaryResponse(
        UserResponse profile,
        List<EnrollmentResponse> enrollments,
        Double attendanceRate,
        List<GradeResponse> grades,
        Double overallGrade,
        BigDecimal paymentBalance,
        List<CertificateResponse> certificates
) {
}
