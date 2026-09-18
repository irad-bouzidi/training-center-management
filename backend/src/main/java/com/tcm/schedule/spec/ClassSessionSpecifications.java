package com.tcm.schedule.spec;

import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.schedule.model.ClassSession;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Building blocks for {@code ClassSessionRepository}'s filterable search,
 * same pattern as {@code EnrollmentSpecifications}: each method returns
 * {@code null} for a "no-op" predicate when its filter isn't supplied, so
 * callers can chain every filter unconditionally via {@link Specification#and}.
 */
public final class ClassSessionSpecifications {

    private ClassSessionSpecifications() {
    }

    public static Specification<ClassSession> hasCourse(UUID courseId) {
        return (root, query, cb) -> courseId == null ? null : cb.equal(root.get("course").get("id"), courseId);
    }

    public static Specification<ClassSession> hasTrainer(UUID trainerId) {
        return (root, query, cb) -> trainerId == null ? null : cb.equal(root.get("trainer").get("id"), trainerId);
    }

    /** Inclusive lower bound on {@code sessionDate}. */
    public static Specification<ClassSession> from(LocalDate from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("sessionDate"), from);
    }

    /** Inclusive upper bound on {@code sessionDate}. */
    public static Specification<ClassSession> to(LocalDate to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("sessionDate"), to);
    }

    /**
     * Restricts to sessions of courses the student is APPROVED in - what a
     * STUDENT caller is allowed to see, per
     * docs/tasks/TCM-17-course-scheduling-api.md. A PENDING or rejected
     * request doesn't reveal the timetable.
     */
    public static Specification<ClassSession> courseApprovedForStudent(UUID studentId) {
        return (root, query, cb) -> {
            var subquery = query.subquery(UUID.class);
            var enrollment = subquery.from(Enrollment.class);
            subquery.select(enrollment.get("course").get("id"))
                    .where(cb.and(
                            cb.equal(enrollment.get("student").get("id"), studentId),
                            cb.equal(enrollment.get("status"), EnrollmentStatus.APPROVED)));
            return root.get("course").get("id").in(subquery);
        };
    }
}
