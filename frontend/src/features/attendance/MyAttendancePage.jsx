import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatSessionDate, formatTimeRange } from '@/features/schedule/scheduleDisplay'
import { formatRate, statusBadgeVariant, summarizeByCourse } from './attendanceDisplay'
import { useMyAttendanceQuery } from './hooks'

/**
 * The student's own attendance (/student/attendance): a per-course summary
 * on top, then every record, newest first, saying whether the trainer marked
 * it or the student scanned in. Only sessions someone actually marked appear
 * - an unmarked session is not an absence.
 */
export function MyAttendancePage() {
  const { t } = useTranslation('attendance')
  const { data: records = [], isLoading, isError } = useMyAttendanceQuery()
  const courses = summarizeByCourse(records)

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('myPage.title')}</CardTitle>
        <CardDescription>{t('myPage.description')}</CardDescription>
      </CardHeader>

      <CardContent className="space-y-6">
        {isLoading && <p className="text-sm text-muted-foreground">{t('common:states.loading')}</p>}
        {isError && <p className="text-sm text-destructive">{t('myPage.loadError')}</p>}
        {!isLoading && !isError && records.length === 0 && (
          <p className="text-sm text-muted-foreground">{t('myPage.empty')}</p>
        )}

        {courses.length > 0 && (
          <section className="space-y-2">
            <h2 className="text-sm font-semibold">{t('myPage.byCourse')}</h2>
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
            <h2 className="text-sm font-semibold">{t('myPage.sessions')}</h2>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('columns.date')}</TableHead>
                  <TableHead>{t('columns.time')}</TableHead>
                  <TableHead>{t('columns.course')}</TableHead>
                  <TableHead>{t('columns.status')}</TableHead>
                  <TableHead>{t('columns.method')}</TableHead>
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
                      <Badge variant={statusBadgeVariant(record.status)}>
                        {t(`common:enums.attendanceStatus.${record.status}`)}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline">{record.method === 'QR' ? t('method.QR') : t('method.MANUAL')}</Badge>
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
