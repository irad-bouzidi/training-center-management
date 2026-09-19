import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatRate } from './attendanceDisplay'
import { useCourseAttendanceReportQueries } from './hooks'

/**
 * A student's attendance broken down per course, for the "Attendance" tab of
 * StudentSummaryPage - per docs/tasks/TCM-20-frontend-attendance.md step 4.
 *
 * There is no per-student attendance endpoint: TCM-19 exposes the overall
 * rate (on the summary itself) and the per-course report. So the breakdown is
 * assembled from one report per approved course, picking this student's row
 * out of each. A trainer viewing the page may not report on courses they
 * don't teach; those rows say so instead of failing the whole tab.
 */
export function StudentAttendanceTab({ studentId, enrollments, overallRate }) {
  const approved = enrollments.filter((enrollment) => enrollment.status === 'APPROVED')
  const reports = useCourseAttendanceReportQueries(approved.map((enrollment) => enrollment.course.id))

  if (approved.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        This student isn’t approved on any course yet, so there is nothing to attend.
      </p>
    )
  }

  return (
    <div className="space-y-3">
      <p className="text-sm text-muted-foreground">
        Overall attendance: <span className="font-medium text-foreground">{formatRate(overallRate)}</span> — late
        arrivals count as attended, and only sessions the student was marked for are counted.
      </p>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Course</TableHead>
            <TableHead className="text-right">Present</TableHead>
            <TableHead className="text-right">Late</TableHead>
            <TableHead className="text-right">Absent</TableHead>
            <TableHead className="text-right">Marked</TableHead>
            <TableHead className="text-right">Rate</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {approved.map((enrollment, index) => {
            const { data: report, isLoading, isError, error } = reports[index]
            const row = report?.students.find((student) => student.studentId === studentId)

            return (
              <TableRow key={enrollment.id}>
                <TableCell>
                  {enrollment.course.name}{' '}
                  <span className="text-xs text-muted-foreground">{enrollment.course.code}</span>
                </TableCell>

                {isLoading || isError || !row ? (
                  <TableCell colSpan={5} className="text-right text-xs text-muted-foreground">
                    {isLoading && 'Loading…'}
                    {isError &&
                      (error.response?.status === 403
                        ? 'Only this course’s trainers and administrators can see its attendance'
                        : 'Unavailable')}
                    {!isLoading && !isError && !row && 'Not on this course’s roster'}
                  </TableCell>
                ) : (
                  <>
                    <TableCell className="text-right tabular-nums">{row.present}</TableCell>
                    <TableCell className="text-right tabular-nums">{row.late}</TableCell>
                    <TableCell className="text-right tabular-nums">{row.absent}</TableCell>
                    <TableCell className="text-right tabular-nums">
                      {row.marked} / {report.sessionCount}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">{formatRate(row.attendanceRate)}</TableCell>
                  </>
                )}
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
    </div>
  )
}
