package com.tcm.payment.spec;

import com.tcm.payment.model.Payment;
import com.tcm.payment.model.PaymentStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Building blocks for the admin payment listing, same pattern as
 * {@code EnrollmentSpecifications}: each method returns {@code null} for a
 * "no-op" predicate when its filter isn't supplied, so callers can chain
 * every filter unconditionally via {@link Specification#and}.
 */
public final class PaymentSpecifications {

    private PaymentSpecifications() {
    }

    public static Specification<Payment> hasStudent(UUID studentId) {
        return (root, query, cb) -> studentId == null ? null : cb.equal(root.get("student").get("id"), studentId);
    }

    public static Specification<Payment> hasCourse(UUID courseId) {
        return (root, query, cb) -> courseId == null ? null : cb.equal(root.get("course").get("id"), courseId);
    }

    public static Specification<Payment> hasStatus(PaymentStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}
