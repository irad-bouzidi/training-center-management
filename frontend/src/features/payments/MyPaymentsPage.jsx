import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { PaymentsTable } from './PaymentsTable'
import { formatAmount, totalOutstanding } from './paymentDisplay'
import { useMyPaymentsQuery } from './hooks'

/**
 * The student's own invoices and balance (/student/payments) - see
 * docs/tasks/TCM-22-frontend-payment.md step 4. Read-only by design: fees are
 * recorded by the office, and this is fee tracking rather than collection.
 */
export function MyPaymentsPage() {
  const { t } = useTranslation('payments')
  const { data: payments = [], isLoading } = useMyPaymentsQuery()

  const outstanding = totalOutstanding(payments)
  const hasOverdue = payments.some((payment) => payment.status === 'OVERDUE')

  return (
    <Card>
      <CardHeader className="flex-row items-start justify-between">
        <div>
          <CardTitle>{t('myPayments.title')}</CardTitle>
          <CardDescription>{t('myPayments.description')}</CardDescription>
        </div>
        <div className="text-right">
          <p className="text-xs text-muted-foreground">{t('myPayments.outstandingBalance')}</p>
          <p className="text-2xl font-semibold tabular-nums">{formatAmount(outstanding)}</p>
          {hasOverdue && (
            <Badge variant="destructive" className="mt-1">
              {t('myPayments.overdue')}
            </Badge>
          )}
        </div>
      </CardHeader>

      <CardContent>
        <PaymentsTable
          payments={payments}
          isLoading={isLoading}
          showStudent={false}
          emptyMessage={t('myPayments.empty')}
        />
      </CardContent>
    </Card>
  )
}
