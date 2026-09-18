/**
 * Enrollment lifecycle, mirroring com.tcm.enrollment.model.EnrollmentStatus
 * (TCM-14): PENDING on creation, an ADMIN decides APPROVED/REJECTED,
 * CANCELLED by the student/an ADMIN, COMPLETED set later by course-completion
 * logic.
 */
export const STATUS_OPTIONS = ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED']

/** PENDING -> "Pending", APPROVED -> "Approved". */
export function titleCase(value) {
  return value.charAt(0) + value.slice(1).toLowerCase()
}

export function statusBadgeVariant(status) {
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

/** Matches EnrollmentServiceImpl#cancel: only a live registration can be
 * withdrawn, a decided or already-cancelled one can't. */
export function isCancellable(status) {
  return status === 'PENDING' || status === 'APPROVED'
}

/**
 * Why the catalog's "Enroll" button is unavailable for a course, or null when
 * the student can enroll. `enrollment` is that student's existing enrollment
 * for the course, if any.
 *
 * Any existing enrollment blocks re-registration, whatever its status -
 * EnrollmentServiceImpl#register rejects on the (student, course) pair alone,
 * so a rejected or cancelled row is a dead end rather than a retry (there's
 * no re-apply flow in the brief).
 */
export function enrollBlockedReason(course, enrollment) {
  if (enrollment) {
    switch (enrollment.status) {
      case 'PENDING':
        return 'Pending approval'
      case 'APPROVED':
        return 'Already enrolled'
      case 'COMPLETED':
        return 'Completed'
      case 'REJECTED':
        return 'Rejected'
      default:
        return 'Cancelled'
    }
  }
  return course.approvedCount >= course.capacity ? 'Full' : null
}

export function formatDate(isoString) {
  return new Date(isoString).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}
