import { ArrowLeft } from 'lucide-react'
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { formatSessionDate, formatTimeRange } from '@/features/schedule/scheduleDisplay'
import { STATUS_OPTIONS, titleCase } from './attendanceDisplay'
import { useMarkAttendanceMutation, useSessionRosterQuery } from './hooks'

/**
 * Take attendance for one session, per
 * docs/tasks/TCM-20-frontend-attendance.md step 2. Mounted under both the
 * trainer's and the admin's layout; the backend lets an admin mark any
 * session and a trainer only their own, and answers 403 otherwise, so this
 * page doesn't second-guess it.
 *
 * Marks are held locally until "Save", then submitted as one bulk request -
 * a roster is read and judged as a whole, and one save keeps it atomic.
 * Students left unmarked are simply not sent, which leaves them unmarked
 * rather than recording an absence nobody asserted.
 */
export function MarkAttendancePage() {
  const { sessionId } = useParams()
  const navigate = useNavigate()
  const { data: roster, isLoading, isError, error } = useSessionRosterQuery(sessionId)
  const markAttendance = useMarkAttendanceMutation(sessionId)
  // Only what this viewer has touched. What's already recorded is read
  // straight off the roster, so nothing has to be copied into state when it
  // arrives and a refetch after saving needs no reconciling.
  const [overrides, setOverrides] = useState({})

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">Loading…</p>
  }

  if (isError) {
    return (
      <p className="text-sm text-muted-foreground">
        {error.response?.status === 403
          ? 'You can only take attendance for sessions you are assigned to.'
          : 'This session could not be loaded.'}
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

  function save() {
    markAttendance.mutate(marked.map((entry) => ({ studentId: entry.studentId, status: markOf(entry) })))
  }

  return (
    <div className="space-y-4">
      <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
        <ArrowLeft />
        Back
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
            <Button variant="outline" size="sm" disabled={entries.length === 0} onClick={markAllPresent}>
              Mark all present
            </Button>
            <Button size="sm" disabled={!isDirty || markAttendance.isPending} onClick={save}>
              {markAttendance.isPending ? 'Saving…' : 'Save'}
            </Button>
          </div>
        </CardHeader>

        <CardContent className="space-y-4">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Student</TableHead>
                <TableHead>Email</TableHead>
                <TableHead className="w-64">Attendance</TableHead>
                <TableHead>Recorded</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {entries.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4} className="text-center text-muted-foreground">
                    Nobody is approved on this course yet, so there is no roster to mark.
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
                      aria-label={`Attendance for ${entry.studentName}`}
                    >
                      {STATUS_OPTIONS.map((status) => (
                        <ToggleGroupItem key={status} value={status}>
                          {titleCase(status)}
                        </ToggleGroupItem>
                      ))}
                    </ToggleGroup>
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {entry.status ? (
                      <>
                        {titleCase(entry.status)}
                        {entry.method === 'QR' && (
                          <Badge variant="outline" className="ml-2">
                            QR
                          </Badge>
                        )}
                      </>
                    ) : (
                      'Not recorded'
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          <p className="text-sm text-muted-foreground">
            {marked.length} of {entries.length} marked
            {isDirty ? ' · unsaved changes' : ''}
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
