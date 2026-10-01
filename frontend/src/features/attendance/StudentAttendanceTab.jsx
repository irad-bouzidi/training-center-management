import { Trans, useTranslation } from 'react-i18next'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatRate } from './attendanceDisplay'
import { useCourseAttendanceReportQueries } from './hooks'

/**
 * A student's attendance broken down per course, for the "Attendance" tab of
 * StudentSummaryPage - per docs/tasks/TCM-20-frontend-attendance.md step 4.
 *
 * There is no per-student attendance endpoint: TCM-19 exposes the overall
 * rate (on the summary itself) and the per-course report. So the breakdown is
 * assembled from one report per course the student is approved on or has
 * completed (a finished course's record still counts - it is what a
 * certificate is judged on), picking this student's row out of each. A trainer viewing the page may not report on courses they
 * don't teach; those rows say so instead of failing the whole tab.
 */
export function StudentAttendanceTab({ studentId, enrollments, overallRate }) {
  const { t } = useTranslation('attendance')
  const attended = enrollments.filter(
    (enrollment) => enrollment.status === 'APPROVED' || enrollment.status === 'COMPLETED',
  )
  const reports = useCourseAttendanceReportQueries(attended.map((enrollment) => enrollment.course.id))

  if (attended.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        {t('studentTab.empty')}
      </p>
    )
  }

  return (
    <div className="space-y-3">
      <p className="text-sm text-muted-foreground">
        <Trans
          t={t}
          i18nKey="studentTab.overall"
          values={{ rate: formatRate(overallRate) }}
          components={{ rate: <span className="font-medium text-foreground" /> }}
        />
      </p>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t('columns.course')}</TableHead>
            <TableHead className="text-right">{t('columns.present')}</TableHead>
            <TableHead className="text-right">{t('columns.late')}</TableHead>
            <TableHead className="text-right">{t('columns.absent')}</TableHead>
            <TableHead className="text-right">{t('columns.marked')}</TableHead>
            <TableHead className="text-right">{t('columns.rate')}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {attended.map((enrollment, index) => {
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
                    {isLoading && t('common:states.loading')}
                    {isError &&
                      (error.response?.status === 403
                        ? t('studentTab.forbidden')
                        : t('studentTab.unavailable'))}
                    {/* The course report is built from its roster; a backend
                        that lists only APPROVED students there leaves a
                        completed one out, which is not the same as never
                        having been on the course. */}
                    {!isLoading &&
                      !isError &&
                      !row &&
                      (enrollment.status === 'COMPLETED'
                        ? t('studentTab.completedNotInReport')
                        : t('studentTab.notOnRoster'))}
                  </TableCell>
                ) : (
                  <>
                    <TableCell className="text-right tabular-nums">{row.present}</TableCell>
                    <TableCell className="text-right tabular-nums">{row.late}</TableCell>
                    <TableCell className="text-right tabular-nums">{row.absent}</TableCell>
                    <TableCell className="text-right tabular-nums">
                      {t('markedOfSessions', { marked: row.marked, total: report.sessionCount })}
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
