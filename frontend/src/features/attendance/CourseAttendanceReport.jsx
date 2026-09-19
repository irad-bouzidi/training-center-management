import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatRate, rateBarClass } from './attendanceDisplay'
import { useCourseAttendanceReportQuery } from './hooks'

function RateBar({ rate }) {
  if (rate === null || rate === undefined) {
    return <span className="text-muted-foreground">—</span>
  }

  return (
    <div className="flex items-center gap-2">
      <div className="h-1.5 w-24 overflow-hidden rounded-full bg-muted">
        <div className={`h-full ${rateBarClass(rate)}`} style={{ width: `${rate}%` }} />
      </div>
      <span className="text-xs tabular-nums">{formatRate(rate)}</span>
    </div>
  )
}

/**
 * Per-student attendance across one course's sessions, per
 * docs/tasks/TCM-20-frontend-attendance.md step 3. Rendered both as the
 * course detail page's "Attendance" tab and as the standalone
 * /admin/courses/:courseId/attendance page. Readable by an ADMIN or a
 * trainer of the course; anyone else gets a 403 from the backend, which
 * reads here as a plain explanation rather than an error.
 */
export function CourseAttendanceReport({ courseId }) {
  const { data: report, isLoading, isError, error } = useCourseAttendanceReportQuery(courseId)

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">Loading…</p>
  }

  if (isError) {
    return (
      <p className="text-sm text-muted-foreground">
        {error.response?.status === 403
          ? 'Attendance reporting is open to administrators and the course’s own trainers.'
          : 'This report could not be loaded.'}
      </p>
    )
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Badge variant="outline">{report.sessionCount} session{report.sessionCount === 1 ? '' : 's'}</Badge>
        <span>
          Rates count late arrivals as attended and are measured against the sessions each student was actually
          marked for.
        </span>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Student</TableHead>
            <TableHead className="text-right">Present</TableHead>
            <TableHead className="text-right">Late</TableHead>
            <TableHead className="text-right">Absent</TableHead>
            <TableHead className="text-right">Marked</TableHead>
            <TableHead className="w-44">Attendance</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {report.students.length === 0 && (
            <TableRow>
              <TableCell colSpan={6} className="text-center text-muted-foreground">
                Nobody is approved on this course yet.
              </TableCell>
            </TableRow>
          )}

          {report.students.map((student) => (
            <TableRow key={student.studentId}>
              <TableCell>
                <p className="font-medium">{student.studentName}</p>
                <p className="text-xs text-muted-foreground">{student.email}</p>
              </TableCell>
              <TableCell className="text-right tabular-nums">{student.present}</TableCell>
              <TableCell className="text-right tabular-nums">{student.late}</TableCell>
              <TableCell className="text-right tabular-nums">{student.absent}</TableCell>
              <TableCell className="text-right tabular-nums">
                {student.marked} / {report.sessionCount}
              </TableCell>
              <TableCell>
                <RateBar rate={student.attendanceRate} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}
