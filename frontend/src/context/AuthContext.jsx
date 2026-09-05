import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { fetchCurrentUser, login as loginRequest } from '@/api/authApi'
import { AUTH_EXPIRED_EVENT, TOKEN_STORAGE_KEY } from '@/api/client'

const AuthContext = createContext(undefined)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_STORAGE_KEY))
  const [user, setUser] = useState(null)
  const [isLoading, setIsLoading] = useState(true)

  // Hydrate on load: if a token survived a refresh, fetch the profile it
  // belongs to. Runs once - login()/logout() update state directly instead
  // of re-triggering this.
  useEffect(() => {
    let cancelled = false

    if (!token) {
      setIsLoading(false)
      return undefined
    }

    fetchCurrentUser()
      .then((profile) => {
        if (!cancelled) setUser(profile)
      })
      .catch(() => {
        if (!cancelled) {
          localStorage.removeItem(TOKEN_STORAGE_KEY)
          setToken(null)
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // A protected request 401'd (token expired/revoked mid-session) - drop the
  // session so ProtectedRoute's render-time check sends the user to /login.
  useEffect(() => {
    function handleAuthExpired() {
      setToken(null)
      setUser(null)
    }

    window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired)
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired)
  }, [])

  const login = useCallback(async (email, password) => {
    const response = await loginRequest(email, password)
    localStorage.setItem(TOKEN_STORAGE_KEY, response.token)
    setToken(response.token)
    setUser(response.user)
    return response.user
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    setToken(null)
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, token, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
