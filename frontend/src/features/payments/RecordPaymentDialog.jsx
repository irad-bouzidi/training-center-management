import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
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
import { errorMessage, useRecordPaymentMutation } from './hooks'

/**
 * Mirrors backend/src/main/java/com/tcm/payment/dto/PaymentTransactionRequest.java.
 * The "not more than is outstanding" rule is checked here as well as
 * server-side so the admin finds out before a round trip - the server has the
 * last word, and says so inline below.
 */
function schema(outstanding) {
  return z.object({
    amount: z.coerce
      .number({ message: 'Amount is required' })
      .positive('Amount must be greater than zero')
      .max(outstanding, `Only ${formatAmount(outstanding)} is still owed on this invoice`),
    paymentMethod: z.string().max(50).optional(),
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
        onError: (error) => setServerError(errorMessage(error, 'Failed to record payment')),
      },
    )
  }

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <DialogHeader>
            <DialogTitle>Record payment</DialogTitle>
            <DialogDescription>
              {payment.student.name} · {payment.course.name} · {formatAmount(outstanding)} outstanding of{' '}
              {formatAmount(payment.amountDue)}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-2">
            <Label htmlFor="amount">Amount</Label>
            <Input id="amount" type="number" step="0.01" min="0.01" max={outstanding} {...register('amount')} />
            {errors.amount && <p className="text-sm text-destructive">{errors.amount.message}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="paymentMethod">Method</Label>
            <Input id="paymentMethod" placeholder="Cash, card, transfer…" {...register('paymentMethod')} />
            {errors.paymentMethod && <p className="text-sm text-destructive">{errors.paymentMethod.message}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="notes">Notes</Label>
            <Textarea id="notes" rows={3} {...register('notes')} />
          </div>

          {serverError && <p className="text-sm text-destructive">{serverError}</p>}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting || recordPayment.isPending}>
              {recordPayment.isPending ? 'Recording…' : 'Record payment'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
