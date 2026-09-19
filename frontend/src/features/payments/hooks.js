import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { listMyPayments, listPayments, recordPayment } from '@/api/paymentApi'
import { studentsKeys } from '@/features/students/hooks'

export const paymentsKeys = {
  all: ['payments'],
  lists: () => [...paymentsKeys.all, 'list'],
  list: (params) => [...paymentsKeys.lists(), params],
  mine: () => [...paymentsKeys.all, 'mine'],
}

// The backend never has a message body it can't produce (see
// GlobalExceptionHandler) - falling back to a generic string only covers a
// network-level failure (no response at all).
export function errorMessage(error, fallback) {
  return error.response?.data?.message ?? fallback
}

export function usePaymentsQuery(params, options) {
  return useQuery({
    queryKey: paymentsKeys.list(params),
    queryFn: () => listPayments(params),
    // Keeps the current page's rows on screen while the next page loads,
    // instead of the table flashing empty between pages/filter changes.
    placeholderData: keepPreviousData,
    ...options,
  })
}

/** The STUDENT's own invoices; 403s for anyone else, hence `options`. */
export function useMyPaymentsQuery(options) {
  return useQuery({
    queryKey: paymentsKeys.mine(),
    queryFn: listMyPayments,
    ...options,
  })
}

/**
 * Recording a payment moves the invoice's status and the student's balance,
 * which the student summary reads too - so both go stale together.
 *
 * No error toast: this is submitted from RecordPaymentDialog, which stays
 * open on failure and shows the server's message inline, next to the amount
 * it's about.
 */
export function useRecordPaymentMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, ...payload }) => recordPayment(id, payload),
    onSuccess: (payment) => {
      queryClient.invalidateQueries({ queryKey: paymentsKeys.all })
      queryClient.invalidateQueries({ queryKey: studentsKeys.all })
      toast.success(payment.status === 'PAID' ? 'Invoice settled' : 'Payment recorded')
    },
  })
}
