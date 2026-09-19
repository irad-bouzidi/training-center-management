package com.tcm.payment.model;

/**
 * Invoice lifecycle, per docs/PLAN.md §5. An invoice opens PENDING, becomes
 * PARTIAL once something has been paid against it and PAID when it's settled
 * in full. OVERDUE is PENDING or PARTIAL past its due date, and is set by
 * {@code PaymentService#markOverdueSweep} rather than by a payment - it's a
 * fact about the calendar, not about a transaction.
 */
public enum PaymentStatus {
    PENDING,
    PARTIAL,
    PAID,
    OVERDUE
}
