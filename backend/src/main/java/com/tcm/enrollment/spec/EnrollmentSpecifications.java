package com.tcm.enrollment.spec;

import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Building blocks for {@code EnrollmentRepository}'s filterable search, same
 * pattern as {@code CourseSpecifications}: each method returns {@code null}
 * for a "no-op" predicate when its filter isn't supplied, so callers can
 * chain every filter unconditionally via {@link Specification#and}.
 */
public final class EnrollmentSpecifications {

    private EnrollmentSpecifications() {
    }

    public static Specification<Enrollment> hasCourse(UUID courseId) {
        return (root, query, cb) -> courseId == null ? null : cb.equal(root.get("course").get("id"), courseId);
    }

    public static Specification<Enrollment> hasStudent(UUID studentId) {
        return (root, query, cb) -> studentId == null ? null : cb.equal(root.get("student").get("id"), studentId);
    }

    public static Specification<Enrollment> hasStatus(EnrollmentStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    /** Restricts to enrollments in courses trained by the given trainer. */
    public static Specification<Enrollment> hasCourseTrainer(UUID trainerId) {
        return (root, query, cb) -> trainerId == null
                ? null
                : cb.equal(root.get("course").get("primaryTrainer").get("id"), trainerId);
    }
}
