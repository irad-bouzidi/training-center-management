export const ROLE_HOME_PATHS = {
  ADMIN: '/admin',
  TRAINER: '/trainer',
  STUDENT: '/student',
}

export function homePathForRole(role) {
  return ROLE_HOME_PATHS[role] ?? '/login'
}
