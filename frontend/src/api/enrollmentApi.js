import { apiClient } from './client'

/**
 * ADMIN/TRAINER listing - see EnrollmentController#search. A TRAINER caller is
 * restricted server-side to their own courses, so the same call powers the
 * admin approvals queue and a trainer's per-course view.
 *
 * @param {{page?: number, size?: number, sort?: string, courseId?: string, studentId?: string, status?: string}} params
 * @returns {Promise<{content: object[], page: number, size: number, totalElements: number, totalPages: number}>}
 */
export async function listEnrollments(params) {
  const { data } = await apiClient.get('/enrollments', { params })
  return data
}

/**
 * The authenticated STUDENT's own enrollments - see
 * EnrollmentController#mine.
 *
 * @param {{page?: number, size?: number, sort?: string, status?: string}} params
 */
export async function listMyEnrollments(params) {
  const { data } = await apiClient.get('/enrollments/mine', { params })
  return data
}

/** A STUDENT self-registers; `studentId` is ignored for them and only read
 * when an ADMIN registers someone on their behalf. */
export async function registerEnrollment({ courseId, studentId }) {
  const { data } = await apiClient.post('/enrollments', { courseId, studentId })
  return data
}

/** @param {'APPROVED'|'REJECTED'} status */
export async function decideEnrollment(id, status) {
  const { data } = await apiClient.post(`/enrollments/${id}/decision`, { status })
  return data
}

/** A student may only cancel their own enrollment; an ADMIN may cancel any
 * (ownership is enforced server-side). */
export async function cancelEnrollment(id) {
  const { data } = await apiClient.post(`/enrollments/${id}/cancel`)
  return data
}
