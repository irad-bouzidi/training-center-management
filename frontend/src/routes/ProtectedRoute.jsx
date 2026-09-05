import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'

/** Redirects to /login if unauthenticated; otherwise renders the matched
 * nested route via <Outlet/>. */
export function ProtectedRoute() {
  const { token, user, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return null
  }

  if (!token || !user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}
