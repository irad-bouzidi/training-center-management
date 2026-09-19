package com.tcm.payment.dto;

import com.tcm.payment.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        StudentSummary student,
        CourseSummary course,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        BigDecimal outstanding,
        PaymentStatus status,
        LocalDate dueDate,
        Instant paidAt,
        String paymentMethod,
        String notes
) {
    public record StudentSummary(UUID id, String name, String email) {
    }

    public record CourseSummary(UUID id, String code, String name) {
    }
}
