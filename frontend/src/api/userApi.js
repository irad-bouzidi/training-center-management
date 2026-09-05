import { apiClient } from './client'

/**
 * @param {{page?: number, size?: number, role?: string, status?: string, name?: string}} params
 * @returns {Promise<{content: object[], page: number, size: number, totalElements: number, totalPages: number}>}
 */
export async function listUsers(params) {
  const { data } = await apiClient.get('/users', { params })
  return data
}

/** @returns {Promise<object>} */
export async function getUser(id) {
  const { data } = await apiClient.get(`/users/${id}`)
  return data
}

/** @param {{firstName: string, lastName: string, email: string, password: string, phone?: string, role: string}} payload */
export async function createUser(payload) {
  const { data } = await apiClient.post('/users', payload)
  return data
}

/** @param {{firstName: string, lastName: string, email: string, phone?: string, role: string}} payload
 * password is deliberately not part of this payload - see resetPassword(). */
export async function updateUser(id, payload) {
  const { data } = await apiClient.put(`/users/${id}`, payload)
  return data
}

/** @param {'ACTIVE'|'INACTIVE'} status */
export async function setUserStatus(id, status) {
  const { data } = await apiClient.patch(`/users/${id}/status`, { status })
  return data
}

/** @returns {Promise<{tempPassword: string}>} */
export async function resetPassword(id) {
  const { data } = await apiClient.post(`/users/${id}/reset-password`)
  return data
}
