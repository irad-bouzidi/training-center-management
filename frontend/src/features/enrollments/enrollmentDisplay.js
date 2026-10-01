import { formatDateTimeAsDate } from '@/lib/format'

/**
 * Enrollment lifecycle, mirroring com.tcm.enrollment.model.EnrollmentStatus
 * (TCM-14): PENDING on creation, an ADMIN decides APPROVED/REJECTED,
 * CANCELLED by the student/an ADMIN, COMPLETED set later by course-completion
 * logic.
 */
export const STATUS_OPTIONS = ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED']

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
 * Why the catalog's "Enroll" button is unavailable for a course - as an
 * `enrollments:` i18n key, for the caller to translate - or null when the
 * student can enroll. `enrollment` is that student's existing enrollment
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
        return 'enroll.blocked.pending'
      case 'APPROVED':
        return 'enroll.blocked.approved'
      case 'COMPLETED':
        return 'enroll.blocked.completed'
      case 'REJECTED':
        return 'enroll.blocked.rejected'
      default:
        return 'enroll.blocked.cancelled'
    }
  }
  return course.approvedCount >= course.capacity ? 'enroll.blocked.full' : null
}

// Follows the UI language (see @/lib/format), not the browser's.
export function formatDate(isoString) {
  return formatDateTimeAsDate(isoString)
}
