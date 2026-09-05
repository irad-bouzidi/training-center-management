import { Navigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { homePathForRole } from '@/lib/roleHomePaths'

/** `/` itself renders nothing - it only ever redirects to the caller's
 * role-appropriate home. Rendered inside ProtectedRoute, so `user` exists. */
export function RootRedirect() {
  const { user } = useAuth()
  return <Navigate to={homePathForRole(user.role)} replace />
}
