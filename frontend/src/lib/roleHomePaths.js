export const ROLE_HOME_PATHS = {
  ADMIN: '/admin',
  TRAINER: '/trainer',
  STUDENT: '/student',
}

export function homePathForRole(role) {
  return ROLE_HOME_PATHS[role] ?? '/login'
}

/** Every role mounts the course catalog/detail routes at its own
 * `<home>/courses` prefix - see CourseCatalogPage and CourseDetailPage. */
export function courseListPathForRole(role) {
  return `${homePathForRole(role)}/courses`
}

export function courseDetailPathForRole(role, id) {
  return `${courseListPathForRole(role)}/${id}`
}

/** Admin and Trainer both mount the student directory/summary routes at
 * their own `<home>/students` prefix - see StudentsListPage/StudentSummaryPage. */
export function studentListPathForRole(role) {
  return `${homePathForRole(role)}/students`
}

export function studentDetailPathForRole(role, id) {
  return `${studentListPathForRole(role)}/${id}`
}

/**
 * Where to send someone after they sign in, given the location ProtectedRoute
 * bounced them from (its `state.from`). Honoured only when that role may
 * actually open it - their own `<home>` subtree, or the shared QR check-in
 * route - so signing in as a different role than the link was meant for
 * lands on their own home rather than a RoleRoute bounce. The search string
 * rides along: that is what carries a scanned QR code's `?token=` through
 * the login detour.
 */
export function postLoginPathForRole(role, from) {
  const home = homePathForRole(role)
  const pathname = from?.pathname

  if (!pathname) {
    return home
  }

  const isOwnArea = pathname === home || pathname.startsWith(`${home}/`)
  const isCheckIn = pathname === '/attend' || pathname.startsWith('/attend/')

  return isOwnArea || isCheckIn ? `${pathname}${from.search ?? ''}` : home
}
