import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { homePathForRole } from '@/lib/roleHomePaths'

/** Nested inside ProtectedRoute, so `user` is always present here. Redirects
 * to the caller's own home if their role isn't in `allowedRoles`. */
export function RoleRoute({ allowedRoles }) {
  const { user } = useAuth()

  if (!allowedRoles.includes(user.role)) {
    return <Navigate to={homePathForRole(user.role)} replace />
  }

  return <Outlet />
}
