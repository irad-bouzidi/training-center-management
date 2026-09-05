import axios from 'axios'

export const TOKEN_STORAGE_KEY = 'tcm_token'

/** Dispatched on window when a request 401s outside of login itself -
 * AuthContext listens and clears its state, letting ProtectedRoute's normal
 * render-time check redirect to /login (no full page reload). */
export const AUTH_EXPIRED_EVENT = 'tcm:auth-expired'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // A 401 from /auth/login itself just means "wrong credentials" - that's
    // LoginPage's own catch block to handle (toast), not a session expiry.
    const isLoginRequest = error.config?.url?.includes('/auth/login')

    if (error.response?.status === 401 && !isLoginRequest) {
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT))
    }

    return Promise.reject(error)
  },
)
