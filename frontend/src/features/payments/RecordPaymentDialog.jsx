import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { formatAmount } from './paymentDisplay'
import { useRecordPaymentMutation } from './hooks'
import { apiErrorMessage } from '@/api/serverErrors'

/**
 * Mirrors backend/src/main/java/com/tcm/payment/dto/PaymentTransactionRequest.java.
 * The "not more than is outstanding" rule is checked here as well as
 * server-side so the admin finds out before a round trip - the server has the
 * last word, and says so inline below. Messages are i18n keys in the
 * `payments` namespace, translated where they render (the "too much" one
 * takes the outstanding amount as {{amount}}).
 */
function schema(outstanding) {
  return z.object({
    amount: z.coerce
      .number({ message: 'dialog.errors.amountRequired' })
      .positive('dialog.errors.amountPositive')
      .max(outstanding, 'dialog.errors.amountTooHigh'),
    paymentMethod: z.string().max(50, 'dialog.errors.methodTooLong').optional(),
    notes: z.string().optional(),
  })
}

/**
 * Records money received against one invoice (ADMIN) - see
 * docs/tasks/TCM-22-frontend-payment.md step 2. This is fee tracking, not
 * collection: no card details are asked for or sent anywhere.
 *
 * Mounted only while an invoice is being paid (see PaymentsListPage), so the
 * form and its error start empty every time rather than being reset.
 */
export function RecordPaymentDialog({ onOpenChange, payment }) {
  const { t } = useTranslation('payments')
  const [serverError, setServerError] = useState(null)
  const recordPayment = useRecordPaymentMutation()
  const outstanding = Number(payment.outstanding)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(schema(outstanding)),
    defaultValues: { amount: '', paymentMethod: '', notes: '' },
  })

  function onSubmit(values) {
    setServerError(null)
    recordPayment.mutate(
      {
        id: payment.id,
        amount: values.amount,
        paymentMethod: values.paymentMethod || null,
        notes: values.notes || null,
      },
      {
        onSuccess: () => onOpenChange(false),
        // The error is kept and translated at render, so it follows a language switch.
        onError: setServerError,
      },
    )
  }

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <DialogHeader>
            <DialogTitle>{t('dialog.title')}</DialogTitle>
            <DialogDescription>
              {t('dialog.description', {
                student: payment.student.name,
                course: payment.course.name,
                outstanding: formatAmount(outstanding),
                due: formatAmount(payment.amountDue),
              })}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-2">
            <Label htmlFor="amount">{t('dialog.amount')}</Label>
            <Input id="amount" type="number" step="0.01" min="0.01" max={outstanding} {...register('amount')} />
            {errors.amount && <p className="text-sm text-destructive">{t(errors.amount.message, { amount: formatAmount(outstanding) })}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="paymentMethod">{t('dialog.method')}</Label>
            <Input id="paymentMethod" maxLength={50} placeholder={t('dialog.methodPlaceholder')} {...register('paymentMethod')} />
            {errors.paymentMethod && <p className="text-sm text-destructive">{t(errors.paymentMethod.message)}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="notes">{t('dialog.notes')}</Label>
            <Textarea id="notes" rows={3} {...register('notes')} />
          </div>

          {serverError && <p className="text-sm text-destructive">{apiErrorMessage(serverError, t('dialog.failed'))}</p>}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              {t('common:actions.cancel')}
            </Button>
            <Button type="submit" disabled={isSubmitting || recordPayment.isPending}>
              {recordPayment.isPending ? t('dialog.submitting') : t('dialog.submit')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
