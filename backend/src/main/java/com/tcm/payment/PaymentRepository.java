package com.tcm.payment;

import com.tcm.payment.model.Payment;
import com.tcm.payment.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Filtering by student, course or status for the admin listing is
 * {@code PaymentServiceImpl#search}'s job, via {@code PaymentSpecifications}
 * and {@link JpaSpecificationExecutor}. What lives here is what
 * specifications can't express as readably: the overdue sweep's query and
 * the balance roll-up.
 */
public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    /** A student's own invoices, newest due first. */
    List<Payment> findByStudentIdOrderByDueDateDesc(UUID studentId);

    /** Guards the auto-invoice on enrollment approval against a duplicate. */
    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    /**
     * Unsettled invoices whose due date has passed - what
     * {@code PaymentServiceImpl#markOverdueSweep} moves to OVERDUE. A PAID
     * invoice is never overdue however late it was settled, and one already
     * OVERDUE needs no second visit.
     */
    @Query("""
            select p from Payment p
            where p.dueDate < :today and p.status in :unsettled
            """)
    List<Payment> findPastDue(@Param("today") LocalDate today,
                               @Param("unsettled") Collection<PaymentStatus> unsettled);

    /**
     * What a student still owes across every invoice of theirs, for
     * {@code StudentSummaryResponse#paymentBalance}. Null when they have no
     * invoices at all, which the service turns into a zero balance.
     */
    @Query("select sum(p.amountDue - p.amountPaid) from Payment p where p.student.id = :studentId")
    BigDecimal sumOutstandingByStudentId(@Param("studentId") UUID studentId);
}
