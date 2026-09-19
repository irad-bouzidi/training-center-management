package com.tcm.payment;

import com.tcm.common.PageResponse;
import com.tcm.payment.dto.PaymentRequest;
import com.tcm.payment.dto.PaymentResponse;
import com.tcm.payment.dto.PaymentTransactionRequest;
import com.tcm.payment.model.PaymentStatus;
import com.tcm.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fee and payment tracking, per docs/tasks/TCM-21. Everything but
 * {@code /mine} is ADMIN-only: a student may read their own invoices and
 * nothing else, which {@code /mine} enforces by ignoring any identity but
 * the caller's own.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createInvoice(@Valid @RequestBody PaymentRequest request) {
        return paymentService.createInvoice(request);
    }

    @PostMapping("/{id}/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse recordPayment(@PathVariable UUID id,
                                          @Valid @RequestBody PaymentTransactionRequest request) {
        return paymentService.recordPayment(id, request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<PaymentResponse> search(@RequestParam(required = false) UUID studentId,
                                                  @RequestParam(required = false) UUID courseId,
                                                  @RequestParam(required = false) PaymentStatus status,
                                                  @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(paymentService.search(studentId, courseId, status, pageable));
    }

    /** Runs the overdue sweep on its own, for an admin who wants it now. */
    @PostMapping("/overdue-sweep")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> sweepOverdue() {
        return Map.of("updated", paymentService.markOverdueSweep());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public List<PaymentResponse> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.findMineForStudent(principal.getId());
    }
}
