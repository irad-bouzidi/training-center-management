import { apiClient } from './client'

/**
 * @param {{page?: number, size?: number, status?: string, category?: string, trainerId?: string, query?: string}} params
 * @returns {Promise<{content: object[], page: number, size: number, totalElements: number, totalPages: number}>}
 */
export async function listCourses(params) {
  const { data } = await apiClient.get('/courses', { params })
  return data
}

/** @returns {Promise<object>} */
export async function getCourse(id) {
  const { data } = await apiClient.get(`/courses/${id}`)
  return data
}

/** @param {{code: string, name: string, description?: string, durationHours: number, capacity: number, category?: string, primaryTrainerId?: string, price: number, status: string}} payload */
export async function createCourse(payload) {
  const { data } = await apiClient.post('/courses', payload)
  return data
}

/** @param {object} payload - same shape as createCourse's */
export async function updateCourse(id, payload) {
  const { data } = await apiClient.put(`/courses/${id}`, payload)
  return data
}

/** @param {'DRAFT'|'PUBLISHED'|'ARCHIVED'} status */
export async function setCourseStatus(id, status) {
  const { data } = await apiClient.patch(`/courses/${id}/status`, { status })
  return data
}
