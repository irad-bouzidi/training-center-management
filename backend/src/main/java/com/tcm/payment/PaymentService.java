package com.tcm.payment;

import com.tcm.course.model.Course;
import com.tcm.payment.dto.PaymentRequest;
import com.tcm.payment.dto.PaymentResponse;
import com.tcm.payment.dto.PaymentTransactionRequest;
import com.tcm.payment.model.PaymentStatus;
import com.tcm.user.model.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    /** Opens an invoice (ADMIN). The student must have role STUDENT and the course must exist. */
    PaymentResponse createInvoice(PaymentRequest request);

    /**
     * The same, for an enrollment that has just been approved: the course's
     * own price, due after the standard term. Does nothing (returning empty)
     * when the course is free or the pair already has an invoice, so
     * approving stays idempotent from the payment side.
     *
     * @see com.tcm.enrollment.EnrollmentServiceImpl#decide
     */
    void createInvoiceOnApproval(User student, Course course);

    /**
     * Adds money received to the invoice's running total and recomputes its
     * status: PARTIAL while something is still owed, PAID once it's settled
     * (which stamps {@code paidAt}). Paying more than is owed is a
     * {@link com.tcm.common.BadRequestException} - there is no refund path
     * here, so an overpayment would be unrecoverable.
     */
    PaymentResponse recordPayment(UUID paymentId, PaymentTransactionRequest request);

    /**
     * Moves every unsettled, past-due invoice to OVERDUE and returns how many
     * changed. On-demand rather than scheduled (see docs/tasks/TCM-21): the
     * admin listing runs it so what it shows is never stale, and
     * {@code POST /payments/overdue-sweep} exposes it on its own.
     */
    int markOverdueSweep();

    /** The admin listing, filterable by student, course and status. */
    Page<PaymentResponse> search(UUID studentId, UUID courseId, PaymentStatus status, Pageable pageable);

    /** A student's own invoices, newest due first. */
    List<PaymentResponse> findMineForStudent(UUID studentId);

    /**
     * What a student still owes across every invoice of theirs - zero, never
     * null, when they have none. Feeds
     * {@code StudentSummaryResponse#paymentBalance}.
     */
    BigDecimal outstandingBalance(UUID studentId);
}
