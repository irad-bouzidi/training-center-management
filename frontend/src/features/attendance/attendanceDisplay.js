/**
 * Attendance marks, mirroring com.tcm.attendance.model.AttendanceStatus
 * (TCM-19). A student with no record at all is none of these - the roster
 * sends `null` for them, which is why "unmarked" is its own state here
 * rather than a fourth status.
 */
export const STATUS_OPTIONS = ['PRESENT', 'LATE', 'ABSENT']

/** PRESENT -> "Present". */
export function titleCase(value) {
  return value.charAt(0) + value.slice(1).toLowerCase()
}

export function statusBadgeVariant(status) {
  switch (status) {
    case 'PRESENT':
      return 'default'
    case 'LATE':
      return 'secondary'
    case 'ABSENT':
      return 'destructive'
    default:
      return 'outline'
  }
}

/**
 * The backend sends a percentage (66.7) or null while a student has no marks
 * at all - an unmarked student is not a 0% student, so they read as "—".
 */
export function formatRate(rate) {
  return rate === null || rate === undefined ? '—' : `${rate}%`
}

/** Green above 75%, amber above 50%, red below - the bar in the report table. */
export function rateBarClass(rate) {
  if (rate >= 75) {
    return 'bg-primary'
  }
  return rate >= 50 ? 'bg-secondary-foreground/60' : 'bg-destructive'
}
