package com.tcm.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Records money received against an existing invoice. There is no transaction
 * history: the amount is added to the invoice's running total, and the method
 * and notes describe the most recent payment (see {@code Payment}).
 */
public record PaymentTransactionRequest(
        @NotNull @DecimalMin(value = "0.01", message = "must be greater than zero") BigDecimal amount,
        @Size(max = 50) String paymentMethod,
        String notes
) {
}
