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
