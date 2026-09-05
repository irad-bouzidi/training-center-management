package com.tcm.enrollment.model;

/**
 * Enrollment lifecycle, per docs/PLAN.md §5. {@code PENDING} on creation;
 * an ADMIN decides {@code APPROVED}/{@code REJECTED}; the student (or an
 * ADMIN) may {@code CANCELLED} a {@code PENDING}/{@code APPROVED} row;
 * {@code COMPLETED} is set later by course-completion logic (see
 * {@code EnrollmentService#markCompleted}).
 */
public enum EnrollmentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    COMPLETED
}
