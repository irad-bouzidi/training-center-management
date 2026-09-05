package com.tcm.course.spec;

import com.tcm.course.model.Course;
import com.tcm.course.model.CourseStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Building blocks for {@code CourseRepository}'s filterable search. Each
 * method returns {@code null} for a "no-op" predicate when its filter isn't
 * supplied, which {@link Specification#and} and {@link Specification#where}
 * treat as "always true" - so callers can chain every filter unconditionally.
 */
public final class CourseSpecifications {

    private CourseSpecifications() {
    }

    public static Specification<Course> hasStatus(CourseStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Course> hasCategory(String category) {
        return (root, query, cb) -> (category == null || category.isBlank())
                ? null
                : cb.equal(cb.lower(root.get("category")), category.toLowerCase());
    }

    public static Specification<Course> hasTrainer(UUID trainerId) {
        return (root, query, cb) -> trainerId == null
                ? null
                : cb.equal(root.get("primaryTrainer").get("id"), trainerId);
    }

    /** Free-text match against name or code. */
    public static Specification<Course> nameOrCodeContains(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String pattern = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("code")), pattern));
        };
    }
}
