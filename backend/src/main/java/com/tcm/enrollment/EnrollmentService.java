package com.tcm.enrollment;

import com.tcm.enrollment.dto.EnrollmentResponse;
import com.tcm.enrollment.model.EnrollmentStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnrollmentService {

    /**
     * Validates the course is PUBLISHED, capacity isn't already met (count of
     * APPROVED enrollments &lt; course.capacity), and no enrollment already
     * exists for this (student, course) pair, then creates a PENDING one.
     */
    EnrollmentResponse register(UUID studentId, UUID courseId);

    /** @param decision must be APPROVED or REJECTED. */
    EnrollmentResponse decide(UUID enrollmentId, EnrollmentStatus decision, UUID adminId);

    /**
     * @param requesterIsAdmin whether the caller holds ROLE_ADMIN - anyone
     *                         else may only cancel their own enrollment.
     */
    EnrollmentResponse cancel(UUID enrollmentId, UUID requesterId, boolean requesterIsAdmin);

    /**
     * Not yet exposed via a controller endpoint - reserved for the TCM-25
     * certificate flow (or a future scheduled/manual admin action) to call
     * once a student has finished a course.
     */
    EnrollmentResponse markCompleted(UUID enrollmentId);

    /** {@code GET /api/v1/enrollments} (ADMIN) and {@code /mine} (STUDENT, studentId fixed to self). */
    Page<EnrollmentResponse> search(UUID courseId, UUID studentId, EnrollmentStatus status, Pageable pageable);

    /**
     * {@code GET /api/v1/enrollments} for a TRAINER: restricted to enrollments
     * in their own courses. If {@code courseId} is given it must be one of
     * the trainer's own courses, or {@link org.springframework.security.access.AccessDeniedException} is thrown.
     */
    Page<EnrollmentResponse> searchForTrainer(UUID trainerId, UUID courseId, EnrollmentStatus status, Pageable pageable);
}
