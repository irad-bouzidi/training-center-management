import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatAmount, formatDueDate, statusBadgeVariant } from './paymentDisplay'

/**
 * The invoice table, shared by the admin listing, the student summary's
 * Payments tab and the student's own page - the same columns answer all
 * three. `onRecordPayment` is what varies: an admin gets the row action,
 * everyone else reads.
 *
 * `showStudent` is off wherever every row is the same student, since a column
 * of one repeated name tells the reader nothing.
 */
export function PaymentsTable({ payments, isLoading, showStudent = true, onRecordPayment, emptyMessage }) {
  const { t } = useTranslation('payments')
  const columnCount = showStudent ? 7 : 6

  return (
    <Table>
      <TableHeader>
        <TableRow>
          {showStudent && <TableHead>{t('table.student')}</TableHead>}
          <TableHead>{t('table.course')}</TableHead>
          <TableHead className="text-right">{t('table.amountDue')}</TableHead>
          <TableHead className="text-right">{t('table.amountPaid')}</TableHead>
          <TableHead className="text-right">{t('table.outstanding')}</TableHead>
          <TableHead>{t('table.dueDate')}</TableHead>
          <TableHead>{t('table.status')}</TableHead>
          {onRecordPayment && <TableHead className="w-36" />}
        </TableRow>
      </TableHeader>
      <TableBody>
        {isLoading && (
          <TableRow>
            <TableCell colSpan={columnCount + (onRecordPayment ? 1 : 0)} className="text-center text-muted-foreground">
              {t('common:states.loading')}
            </TableCell>
          </TableRow>
        )}

        {!isLoading && payments.length === 0 && (
          <TableRow>
            <TableCell colSpan={columnCount + (onRecordPayment ? 1 : 0)} className="text-center text-muted-foreground">
              {emptyMessage}
            </TableCell>
          </TableRow>
        )}

        {payments.map((payment) => (
          <TableRow key={payment.id}>
            {showStudent && (
              <TableCell>
                <p className="font-medium">{payment.student.name}</p>
                <p className="text-xs text-muted-foreground">{payment.student.email}</p>
              </TableCell>
            )}
            <TableCell>
              {payment.course.name} <span className="text-xs text-muted-foreground">{payment.course.code}</span>
            </TableCell>
            <TableCell className="text-right tabular-nums">{formatAmount(payment.amountDue)}</TableCell>
            <TableCell className="text-right tabular-nums">{formatAmount(payment.amountPaid)}</TableCell>
            <TableCell className="text-right font-medium tabular-nums">{formatAmount(payment.outstanding)}</TableCell>
            <TableCell>{formatDueDate(payment.dueDate)}</TableCell>
            <TableCell>
              <Badge variant={statusBadgeVariant(payment.status)}>{t(`common:enums.paymentStatus.${payment.status}`)}</Badge>
            </TableCell>
            {onRecordPayment && (
              <TableCell>
                {payment.status !== 'PAID' && (
                  <Button variant="outline" size="sm" onClick={() => onRecordPayment(payment)}>
                    {t('table.recordPayment')}
                  </Button>
                )}
              </TableCell>
            )}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
