package com.tcm.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Opens an invoice against a student for a course (ADMIN). */
public record PaymentRequest(
        @NotNull UUID studentId,
        @NotNull UUID courseId,
        @NotNull @DecimalMin(value = "0.01", message = "must be greater than zero") BigDecimal amountDue,
        @NotNull LocalDate dueDate
) {
}
