package com.tcm.user.dto;

/**
 * Row shape for {@code GET /api/v1/students}, per docs/tasks/TCM-13. Wraps
 * the plain {@link UserResponse} with the aggregate counts the student
 * directory table needs; {@code activeEnrollments} is the student's count of
 * APPROVED enrollments (see TCM-14).
 */
public record StudentDirectoryResponse(
        UserResponse profile,
        int activeEnrollments
) {
}
