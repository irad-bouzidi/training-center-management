import axios from 'axios'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
})

// Attaches the auth token to every outgoing request. Real token retrieval
// (from AuthContext / storage) lands in TCM-9; this is just the hook point.
apiClient.interceptors.request.use((config) => {
  const token = null // TODO(TCM-9): read the JWT from the auth session

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})
