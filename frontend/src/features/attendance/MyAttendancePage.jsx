import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatSessionDate, formatTimeRange } from '@/features/schedule/scheduleDisplay'
import { formatRate, statusBadgeVariant, summarizeByCourse, titleCase } from './attendanceDisplay'
import { useMyAttendanceQuery } from './hooks'

/**
 * The student's own attendance (/student/attendance): a per-course summary
 * on top, then every record, newest first, saying whether the trainer marked
 * it or the student scanned in. Only sessions someone actually marked appear
 * - an unmarked session is not an absence.
 */
export function MyAttendancePage() {
  const { data: records = [], isLoading, isError } = useMyAttendanceQuery()
  const courses = summarizeByCourse(records)

  return (
    <Card>
      <CardHeader>
        <CardTitle>My Attendance</CardTitle>
        <CardDescription>
          Every session you’ve been marked for. Late arrivals count as attended.
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-6">
        {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
        {isError && <p className="text-sm text-destructive">Your attendance couldn’t be loaded.</p>}
        {!isLoading && !isError && records.length === 0 && (
          <p className="text-sm text-muted-foreground">No attendance has been recorded for you yet.</p>
        )}

        {courses.length > 0 && (
          <section className="space-y-2">
            <h2 className="text-sm font-semibold">By course</h2>
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
                {courses.map((course) => (
                  <TableRow key={course.courseId}>
                    <TableCell>
                      {course.courseName}{' '}
                      <span className="text-xs text-muted-foreground">{course.courseCode}</span>
                    </TableCell>
                    <TableCell className="text-right tabular-nums">{course.present}</TableCell>
                    <TableCell className="text-right tabular-nums">{course.late}</TableCell>
                    <TableCell className="text-right tabular-nums">{course.absent}</TableCell>
                    <TableCell className="text-right tabular-nums">{course.marked}</TableCell>
                    <TableCell className="text-right tabular-nums">{formatRate(course.attendanceRate)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </section>
        )}

        {records.length > 0 && (
          <section className="space-y-2">
            <h2 className="text-sm font-semibold">Sessions</h2>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead>Time</TableHead>
                  <TableHead>Course</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Method</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {records.map((record) => (
                  <TableRow key={record.id}>
                    <TableCell>{formatSessionDate(record.sessionDate)}</TableCell>
                    <TableCell className="font-mono text-sm">
                      {formatTimeRange(record.startTime, record.endTime)}
                    </TableCell>
                    <TableCell>
                      {record.courseName}{' '}
                      <span className="text-xs text-muted-foreground">{record.courseCode}</span>
                    </TableCell>
                    <TableCell>
                      <Badge variant={statusBadgeVariant(record.status)}>{titleCase(record.status)}</Badge>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline">{record.method === 'QR' ? 'QR' : 'Manual'}</Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </section>
        )}
      </CardContent>
    </Card>
  )
}
