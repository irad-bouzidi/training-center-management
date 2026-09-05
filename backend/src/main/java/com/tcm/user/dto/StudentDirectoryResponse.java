package com.tcm.user.dto;

/**
 * Row shape for {@code GET /api/v1/students}, per docs/tasks/TCM-13. Wraps
 * the plain {@link UserResponse} with the aggregate counts the student
 * directory table needs; {@code activeEnrollments} is a placeholder zero
 * until {@code TCM-14} lands the enrollments table.
 */
public record StudentDirectoryResponse(
        UserResponse profile,
        int activeEnrollments
) {
}
