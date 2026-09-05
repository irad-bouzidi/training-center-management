# TCM-21 — Payment Management API

**Branch**: `TCM-21-payment-api`
**Depends on**: TCM-14

## Goal

Backend to track course fees, payments, balances, and history per
`docs/PLAN.md` §5 `payments`.

## Steps

1. Liquibase changelog: create `payments` (FKs to `users`, `courses`,
   check constraint on `status`).
2. `com.tcm.payment` package:
   - `model/Payment.java`, `model/PaymentStatus.java` (`PENDING`,
     `PARTIAL`, `PAID`, `OVERDUE`).
   - `PaymentRepository` — find by student, by course, overdue query
     (`due_date < now AND status != PAID`).
   - `dto/PaymentRequest.java` (studentId, courseId, amountDue, dueDate) —
     for creating the initial invoice, typically auto-created when an
     enrollment is approved (see step 3).
   - `dto/PaymentTransactionRequest.java` (amount, paymentMethod, notes) —
     for recording a payment against an existing invoice.
   - `dto/PaymentResponse.java`.
   - `PaymentService`/`Impl`:
     - `createInvoice(studentId, courseId, amountDue, dueDate)`.
     - `recordPayment(paymentId, amount, method, notes)` — increments
       `amount_paid`, recomputes `status` (`PARTIAL` if partial,
       `PAID` if `amount_paid >= amount_due`, sets `paid_at` when fully
       paid).
     - `markOverdueSweep()` — recalculates `OVERDUE` status for past-due,
       unpaid invoices (called on-demand from a report endpoint; a
       scheduled job is optional/out of scope).
     - `search` — by student, course, status, paginated (Admin).
     - `findMineForStudent(studentId)`.
   - `PaymentController.java`:
     - `POST /api/v1/payments` (ADMIN) — create invoice.
     - `POST /api/v1/payments/{id}/transactions` (ADMIN) — record a
       payment.
     - `GET /api/v1/payments` (ADMIN, filterable).
     - `GET /api/v1/payments/mine` (STUDENT).
3. Wire enrollment approval (`TCM-14`'s `EnrollmentService.decide`) to
   auto-create a `Payment` invoice from the course's `price` when an
   enrollment is `APPROVED` (only if `price > 0`) — small addition to
   `EnrollmentServiceImpl`, called out explicitly here since it crosses
   packages.
4. Update `StudentSummaryResponse` (`TCM-13`) with real `paymentBalance`.
5. Tests: partial payment transitions status correctly; overpayment is
   rejected (400); overdue sweep correctness.

## Acceptance Criteria

- Approving an enrollment for a priced course creates a payment invoice
  automatically.
- Admin can record payments against an invoice; status transitions
  correctly (`PENDING` → `PARTIAL` → `PAID`).
- Student can view their own payment history/balance.

## Out of Scope

- Real payment gateway integration (out of scope per the brief — this is
  fee/payment *tracking*, not processing).
- Frontend (`TCM-22`).
