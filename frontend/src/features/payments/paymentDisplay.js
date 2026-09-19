/**
 * Invoice lifecycle, mirroring com.tcm.payment.model.PaymentStatus (TCM-21):
 * PENDING on creation, PARTIAL once something has been paid, PAID when
 * settled in full, and OVERDUE for anything still owed past its due date.
 */
export const STATUS_OPTIONS = ['PENDING', 'PARTIAL', 'PAID', 'OVERDUE']

/** PENDING -> "Pending". */
export function titleCase(value) {
  return value.charAt(0) + value.slice(1).toLowerCase()
}

export function statusBadgeVariant(status) {
  switch (status) {
    case 'PAID':
      return 'default'
    case 'OVERDUE':
      return 'destructive'
    case 'PARTIAL':
      return 'secondary'
    default:
      return 'outline'
  }
}

/** The backend sends amounts as JSON numbers from a NUMERIC(10,2). */
export function formatAmount(amount) {
  return Number(amount).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** "31 Jan 2026" - a due date, which the backend sends as yyyy-MM-dd. */
export function formatDueDate(isoDate) {
  return new Date(`${isoDate}T00:00:00`).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

/** What a set of invoices still adds up to. */
export function totalOutstanding(payments) {
  return payments.reduce((total, payment) => total + Number(payment.outstanding), 0)
}
