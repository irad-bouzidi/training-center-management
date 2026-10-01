import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatRate, rateBarClass } from './attendanceDisplay'
import { useCourseAttendanceReportQuery } from './hooks'

function RateBar({ rate }) {
  if (rate === null || rate === undefined) {
    return <span className="text-muted-foreground">{formatRate(rate)}</span>
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
  const { t } = useTranslation('attendance')
  const { data: report, isLoading, isError, error } = useCourseAttendanceReportQuery(courseId)

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">{t('common:states.loading')}</p>
  }

  if (isError) {
    return (
      <p className="text-sm text-muted-foreground">
        {error.response?.status === 403
          ? t('courseReport.forbidden')
          : t('courseReport.loadError')}
      </p>
    )
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Badge variant="outline">{t('courseReport.sessionCount', { count: report.sessionCount })}</Badge>
        <span>{t('courseReport.explanation')}</span>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t('columns.student')}</TableHead>
            <TableHead className="text-right">{t('columns.present')}</TableHead>
            <TableHead className="text-right">{t('columns.late')}</TableHead>
            <TableHead className="text-right">{t('columns.absent')}</TableHead>
            <TableHead className="text-right">{t('columns.marked')}</TableHead>
            <TableHead className="w-44">{t('columns.attendance')}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {report.students.length === 0 && (
            <TableRow>
              <TableCell colSpan={6} className="text-center text-muted-foreground">
                {t('courseReport.empty')}
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
                {t('markedOfSessions', { marked: student.marked, total: report.sessionCount })}
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
