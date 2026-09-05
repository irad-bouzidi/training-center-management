export const STATUS_OPTIONS = ['ACTIVE', 'INACTIVE']

/** ACTIVE -> "Active", PENDING -> "Pending". Shared by user and enrollment status. */
export function titleCase(value) {
  return value.charAt(0) + value.slice(1).toLowerCase()
}

export function fullName(profile) {
  return `${profile.firstName} ${profile.lastName}`
}

export function statusBadgeVariant(status) {
  return status === 'ACTIVE' ? 'secondary' : 'destructive'
}

/**
 * Enrollment lifecycle badge colors - mirrors
 * com.tcm.enrollment.model.EnrollmentStatus (TCM-14): PENDING on creation,
 * an ADMIN decides APPROVED/REJECTED, CANCELLED by the student/an ADMIN,
 * COMPLETED set later by course-completion logic.
 */
export function enrollmentStatusBadgeVariant(status) {
  switch (status) {
    case 'APPROVED':
      return 'secondary'
    case 'COMPLETED':
      return 'default'
    case 'REJECTED':
    case 'CANCELLED':
      return 'destructive'
    default:
      return 'outline'
  }
}

export function formatDate(isoString) {
  return new Date(isoString).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}
