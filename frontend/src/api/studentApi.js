import { apiClient } from './client'

/**
 * @param {{page?: number, size?: number, status?: string, name?: string}} params
 * @returns {Promise<{content: object[], page: number, size: number, totalElements: number, totalPages: number}>}
 */
export async function listStudents(params) {
  const { data } = await apiClient.get('/students', { params })
  return data
}

/**
 * @returns {Promise<object>} StudentSummaryResponse - profile + enrollments
 * (real data as of TCM-14) plus attendanceRate/grades/paymentBalance/
 * certificates stubs later tasks (TCM-20/24/22/26) fill in.
 */
export async function getStudentSummary(id) {
  const { data } = await apiClient.get(`/students/${id}/summary`)
  return data
}
