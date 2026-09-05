# TCM-22 — Frontend Payment Management

**Branch**: `TCM-22-frontend-payment`
**Depends on**: TCM-15, TCM-21

## Goal

Admin UI to track invoices/payments; Student UI to view their own payment
history/balance. Enables the "Payments" tab stub from `TCM-15`.

## Steps

1. `src/api/paymentApi.js` + hooks (`usePaymentsQuery`,
   `useRecordPaymentMutation`, `useMyPaymentsQuery`).
2. `src/features/payments/PaymentsListPage.jsx` (`/admin/payments`) — table
   (student, course, amount due/paid, status badge, due date), filters
   (status, course, student), row action "Record Payment" → `Dialog`
   (amount, method, notes), balance auto-recomputed on success.
3. Populate the "Payments" tab on `StudentSummaryPage` (`TCM-15`) with that
   student's invoices and a running balance.
4. `src/features/payments/MyPaymentsPage.jsx` (`/student/payments`) —
   student's own invoices/history, outstanding balance highlighted (shadcn
   `Badge` variant="destructive" if overdue).
5. Nav entries: "Payments" (Admin), "My Payments" (Student).

## Acceptance Criteria

- Admin can record a payment and see the invoice status/balance update.
- Student sees their own accurate outstanding balance and payment history.

## Out of Scope

- Any real payment collection UI (card entry etc.) — tracking only.
