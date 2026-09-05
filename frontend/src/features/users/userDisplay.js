export const ROLE_OPTIONS = ['ADMIN', 'TRAINER', 'STUDENT']
export const STATUS_OPTIONS = ['ACTIVE', 'INACTIVE']

/** ADMIN -> "Admin", INACTIVE -> "Inactive". */
export function titleCase(value) {
  return value.charAt(0) + value.slice(1).toLowerCase()
}

export function fullName(user) {
  return `${user.firstName} ${user.lastName}`
}

export function formatDate(isoString) {
  return new Date(isoString).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}
