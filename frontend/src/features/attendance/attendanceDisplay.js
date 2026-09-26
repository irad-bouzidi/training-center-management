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

/**
 * A student's own records (GET /attendance/mine) tallied per course, in the
 * same terms as the course report: late counts as attended, and the rate is
 * over the sessions they were marked for, to one decimal like the backend's.
 * Courses keep the order they first appear in - the records come newest
 * first, so the course attended most recently leads.
 *
 * @returns {{courseId: string, courseCode: string, courseName: string, present: number, late: number, absent: number, marked: number, attendanceRate: number}[]}
 */
export function summarizeByCourse(records) {
  const byCourse = new Map()

  for (const record of records) {
    let row = byCourse.get(record.courseId)
    if (!row) {
      row = {
        courseId: record.courseId,
        courseCode: record.courseCode,
        courseName: record.courseName,
        present: 0,
        late: 0,
        absent: 0,
        marked: 0,
      }
      byCourse.set(record.courseId, row)
    }
    row.marked += 1
    if (record.status === 'PRESENT') row.present += 1
    if (record.status === 'LATE') row.late += 1
    if (record.status === 'ABSENT') row.absent += 1
  }

  return [...byCourse.values()].map((row) => ({
    ...row,
    attendanceRate: Math.round(((row.present + row.late) / row.marked) * 1000) / 10,
  }))
}

/** Green above 75%, amber above 50%, red below - the bar in the report table. */
export function rateBarClass(rate) {
  if (rate >= 75) {
    return 'bg-primary'
  }
  return rate >= 50 ? 'bg-secondary-foreground/60' : 'bg-destructive'
}
