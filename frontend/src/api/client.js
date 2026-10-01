import axios from 'axios'
import { parseServerMessage } from './serverErrorRules'

export const TOKEN_STORAGE_KEY = 'tcm_token'

/** Dispatched on window when a request 401s outside of login itself -
 * AuthContext listens and clears its state, letting ProtectedRoute's normal
 * render-time check redirect to /login (no full page reload). */
export const AUTH_EXPIRED_EVENT = 'tcm:auth-expired'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  // Error messages are translated client-side from the backend's English
  // (see serverErrors.js). Without this, Spring would localize the
  // bean-validation part after the browser's language, and the frontend
  // could no longer recognize it.
  headers: { 'Accept-Language': 'en' },
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

    return readApiError(error).then(() => {
      // Recognized once here, translated wherever it is shown - see
      // apiErrorMessage().
      error.serverError = parseServerMessage(error.response?.data?.message)
      return Promise.reject(error)
    })
  },
)

/** A blob request (the certificate download) gets its ApiError as a Blob too. */
async function readApiError(error) {
  const data = error.response?.data
  if (typeof Blob !== 'undefined' && data instanceof Blob && data.type.includes('json')) {
    try {
      error.response.data = JSON.parse(await data.text())
    } catch {
      // Not an ApiError after all - leave the body as it came.
    }
  }
}
