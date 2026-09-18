export const STATUS_OPTIONS = ['ACTIVE', 'INACTIVE']

/** ACTIVE -> "Active", INACTIVE -> "Inactive". */
export function titleCase(value) {
  return value.charAt(0) + value.slice(1).toLowerCase()
}

export function fullName(profile) {
  return `${profile.firstName} ${profile.lastName}`
}

export function statusBadgeVariant(status) {
  return status === 'ACTIVE' ? 'secondary' : 'destructive'
}

export function formatDate(isoString) {
  return new Date(isoString).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}
