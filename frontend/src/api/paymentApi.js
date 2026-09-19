import { apiClient } from './client'

/**
 * The admin invoice listing. Fetching it also runs the overdue sweep
 * server-side (see PaymentServiceImpl#search), so what comes back is never
 * stale on OVERDUE.
 *
 * @param {{page?: number, size?: number, sort?: string, studentId?: string, courseId?: string, status?: string}} params
 * @returns {Promise<{content: object[], page: number, size: number, totalElements: number, totalPages: number}>}
 */
export async function listPayments(params) {
  const { data } = await apiClient.get('/payments', { params })
  return data
}

/**
 * Records money received against an invoice (ADMIN). Paying more than is
 * outstanding is a 400 - there is no refund path.
 *
 * @param {{amount: string, paymentMethod?: string, notes?: string}} payload
 */
export async function recordPayment(paymentId, payload) {
  const { data } = await apiClient.post(`/payments/${paymentId}/transactions`, payload)
  return data
}

/** The signed-in STUDENT's own invoices, newest due first. */
export async function listMyPayments() {
  const { data } = await apiClient.get('/payments/mine')
  return data
}
