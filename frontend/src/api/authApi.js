import { apiClient } from './client'

/** @returns {Promise<{token: string, expiresAt: string, user: {id: string, name: string, email: string, role: string}}>} */
export async function login(email, password) {
  const { data } = await apiClient.post('/auth/login', { email, password })
  return data
}

/** @returns {Promise<{id: string, name: string, email: string, role: string}>} */
export async function fetchCurrentUser() {
  const { data } = await apiClient.get('/auth/me')
  return data
}
