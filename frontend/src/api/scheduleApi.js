import { apiClient } from './client'

/**
 * Class sessions, role-scoped server-side (see ClassSessionController#search):
 * an ADMIN sees every session, a TRAINER only their own - a `trainerId` they
 * pass is ignored rather than honored - and a STUDENT only sessions of
 * courses they hold an APPROVED enrollment in.
 *
 * @param {{page?: number, size?: number, sort?: string, courseId?: string, trainerId?: string, from?: string, to?: string}} params
 *   `from`/`to` are inclusive ISO dates (yyyy-MM-dd).
 * @returns {Promise<{content: object[], page: number, size: number, totalElements: number, totalPages: number}>}
 */
export async function listSessions(params) {
  const { data } = await apiClient.get('/sessions', { params })
  return data
}

/** @param {{courseId: string, trainerId: string, classroom: string, sessionDate: string, startTime: string, endTime: string}} payload */
export async function createSession(payload) {
  const { data } = await apiClient.post('/sessions', payload)
  return data
}

/** @param {object} payload - same shape as createSession's */
export async function updateSession(id, payload) {
  const { data } = await apiClient.put(`/sessions/${id}`, payload)
  return data
}

/** @param {'CANCELLED'|'COMPLETED'} status - CANCELLED is ADMIN-only; COMPLETED
 * is also open to the session's assigned trainer. */
export async function setSessionStatus(id, status) {
  const { data } = await apiClient.patch(`/sessions/${id}/status`, { status })
  return data
}
