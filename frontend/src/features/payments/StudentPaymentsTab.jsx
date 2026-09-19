import { PaymentsTable } from './PaymentsTable'
import { formatAmount, totalOutstanding } from './paymentDisplay'
import { usePaymentsQuery } from './hooks'

// One student's invoices are few enough to show whole rather than paged.
const ALL_FOR_ONE_STUDENT = 100

/**
 * One student's invoices, for the "Payments" tab of StudentSummaryPage - see
 * docs/tasks/TCM-22-frontend-payment.md step 3. The listing is ADMIN-only
 * (PaymentController#search), which is also who reaches this tab; a trainer
 * viewing the same page is told so rather than shown an error.
 */
export function StudentPaymentsTab({ studentId, isAdmin }) {
  const { data, isLoading } = usePaymentsQuery(
    { studentId, size: ALL_FOR_ONE_STUDENT, sort: 'dueDate,desc' },
    { enabled: isAdmin },
  )

  if (!isAdmin) {
    return <p className="text-sm text-muted-foreground">Only administrators can see a student’s invoices.</p>
  }

  const payments = data?.content ?? []

  return (
    <div className="space-y-3">
      <p className="text-sm text-muted-foreground">
        Outstanding balance:{' '}
        <span className="font-medium text-foreground tabular-nums">{formatAmount(totalOutstanding(payments))}</span>
      </p>

      <PaymentsTable
        payments={payments}
        isLoading={isLoading}
        showStudent={false}
        emptyMessage="This student has no invoices."
      />
    </div>
  )
}
