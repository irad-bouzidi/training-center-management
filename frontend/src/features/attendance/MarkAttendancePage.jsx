import { ArrowLeft, QrCode } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useParams } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { formatSessionDate, formatTimeRange } from '@/features/schedule/scheduleDisplay'
import { SessionQrDialog } from './SessionQrDialog'
import { STATUS_OPTIONS } from './attendanceDisplay'
import { useMarkAttendanceMutation, useSessionRosterQuery } from './hooks'

/**
 * Take attendance for one session, per
 * docs/tasks/TCM-20-frontend-attendance.md step 2. Mounted under both the
 * trainer's and the admin's layout; the backend lets an admin mark any
 * session and a trainer only their own, and answers 403 otherwise, so this
 * page doesn't second-guess it.
 *
 * "Show QR" puts the session's code on screen for students to scan
 * themselves in (TCM-27/28); those scans land in the same roster, marked
 * with method QR.
 *
 * Marks are held locally until "Save", then submitted as one bulk request -
 * a roster is read and judged as a whole, and one save keeps it atomic.
 * Only marks this viewer changed are sent: students left unmarked stay
 * unmarked rather than recording an absence nobody asserted, and marks left
 * alone keep who (or which scan) recorded them.
 */
export function MarkAttendancePage() {
  const { t } = useTranslation('attendance')
  const { sessionId } = useParams()
  const navigate = useNavigate()
  const { data: roster, isLoading, isError, error } = useSessionRosterQuery(sessionId)
  const markAttendance = useMarkAttendanceMutation(sessionId)
  // Only what this viewer has touched. What's already recorded is read
  // straight off the roster, so nothing has to be copied into state when it
  // arrives and a refetch after saving needs no reconciling.
  const [overrides, setOverrides] = useState({})
  const [qrOpen, setQrOpen] = useState(false)

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">{t('common:states.loading')}</p>
  }

  if (isError) {
    return (
      <p className="text-sm text-muted-foreground">
        {error.response?.status === 403
          ? t('markPage.forbidden')
          : t('markPage.loadError')}
      </p>
    )
  }

  const { session, entries } = roster
  const markOf = (entry) => overrides[entry.studentId] ?? entry.status
  const marked = entries.filter(markOf)
  const isDirty = entries.some((entry) => markOf(entry) !== entry.status)

  function setMark(studentId, status) {
    // ToggleGroup hands back "" when the pressed item is toggled off; keep
    // the current mark rather than silently clearing it, since the API has
    // no way to un-mark a student.
    if (!status) {
      return
    }
    setOverrides((current) => ({ ...current, [studentId]: status }))
  }

  function markAllPresent() {
    setOverrides(Object.fromEntries(entries.map((entry) => [entry.studentId, 'PRESENT'])))
  }

  // Only rows whose mark differs from what the roster loaded with are sent.
  // Resending an unchanged mark would rewrite it as this viewer's MANUAL
  // mark - turning a student's own QR check-in into one the trainer made.
  function save() {
    const changed = entries.filter((entry) => markOf(entry) && markOf(entry) !== entry.status)
    markAttendance.mutate(
      changed.map((entry) => ({ studentId: entry.studentId, status: markOf(entry) })),
      // Saved marks are the roster's now; dropping the local copies keeps a
      // later change (say, a QR scan) from being masked by a stale override.
      { onSuccess: () => setOverrides({}) },
    )
  }

  return (
    <div className="space-y-4">
      <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
        <ArrowLeft />
        {t('common:actions.back')}
      </Button>

      <Card>
        <CardHeader className="flex-row items-start justify-between">
          <div>
            <CardTitle>{session.course.name}</CardTitle>
            <CardDescription>
              {formatSessionDate(session.sessionDate)} · {formatTimeRange(session.startTime, session.endTime)} ·{' '}
              {session.classroom}
            </CardDescription>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={() => setQrOpen(true)}>
              <QrCode />
              {t('markPage.showQr')}
            </Button>
            <Button variant="outline" size="sm" disabled={entries.length === 0} onClick={markAllPresent}>
              {t('markPage.markAllPresent')}
            </Button>
            <Button size="sm" disabled={!isDirty || markAttendance.isPending} onClick={save}>
              {markAttendance.isPending ? t('common:actions.saving') : t('common:actions.save')}
            </Button>
          </div>
        </CardHeader>

        <CardContent className="space-y-4">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('columns.student')}</TableHead>
                <TableHead>{t('columns.email')}</TableHead>
                <TableHead className="w-64">{t('columns.attendance')}</TableHead>
                <TableHead>{t('columns.recorded')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {entries.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4} className="text-center text-muted-foreground">
                    {t('markPage.empty')}
                  </TableCell>
                </TableRow>
              )}

              {entries.map((entry) => (
                <TableRow key={entry.studentId}>
                  <TableCell className="font-medium">{entry.studentName}</TableCell>
                  <TableCell className="text-muted-foreground">{entry.email}</TableCell>
                  <TableCell>
                    <ToggleGroup
                      type="single"
                      value={markOf(entry) ?? ''}
                      onValueChange={(status) => setMark(entry.studentId, status)}
                      aria-label={t('markPage.rowLabel', { name: entry.studentName })}
                    >
                      {STATUS_OPTIONS.map((status) => (
                        <ToggleGroupItem key={status} value={status}>
                          {t(`common:enums.attendanceStatus.${status}`)}
                        </ToggleGroupItem>
                      ))}
                    </ToggleGroup>
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {entry.status ? (
                      <>
                        {t(`common:enums.attendanceStatus.${entry.status}`)}
                        {entry.method === 'QR' && (
                          <Badge variant="outline" className="ml-2">
                            {t('method.QR')}
                          </Badge>
                        )}
                      </>
                    ) : (
                      t('markPage.notRecorded')
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          <p className="text-sm text-muted-foreground">
            {t(isDirty ? 'markPage.markedCountUnsaved' : 'markPage.markedCount', {
              marked: marked.length,
              total: entries.length,
            })}
          </p>
        </CardContent>
      </Card>

      {qrOpen && <SessionQrDialog onOpenChange={() => setQrOpen(false)} session={session} />}
    </div>
  )
}
