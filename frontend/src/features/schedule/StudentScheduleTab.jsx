import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useAuth } from '@/context/AuthContext'
import { formatSessionDate, formatTimeRange, statusBadgeVariant, titleCase, todayIsoDate } from './scheduleDisplay'
import { useSessionsForCoursesQueries } from './hooks'

// Every session of a course fits one page - a course here is a term of
// classes, not an open-ended stream.
const SESSIONS_PER_COURSE = 200

function SessionsTable({ sessions, emptyMessage }) {
  if (sessions.length === 0) {
    return <p className="text-sm text-muted-foreground">{emptyMessage}</p>
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Date</TableHead>
          <TableHead>Time</TableHead>
          <TableHead>Course</TableHead>
          <TableHead>Classroom</TableHead>
          <TableHead>Trainer</TableHead>
          <TableHead>Status</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {sessions.map((session) => (
          <TableRow key={session.id}>
            <TableCell>{formatSessionDate(session.sessionDate)}</TableCell>
            <TableCell className="font-mono text-sm">{formatTimeRange(session.startTime, session.endTime)}</TableCell>
            <TableCell>
              {session.course.name}{' '}
              <span className="text-xs text-muted-foreground">{session.course.code}</span>
            </TableCell>
            <TableCell>{session.classroom}</TableCell>
            <TableCell>{session.trainer.name}</TableCell>
            <TableCell>
              <Badge variant={statusBadgeVariant(session.status)}>{titleCase(session.status)}</Badge>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}

/**
 * The "Schedule" tab of StudentSummaryPage - see
 * docs/tasks/TCM-18-frontend-scheduling.md step 4. The sessions of every
 * course the student is (or was) on, split into what's ahead and what's
 * been.
 *
 * GET /sessions has no student filter for an admin or trainer, so this asks
 * for each enrolled course's sessions and merges them. The backend's role
 * scoping still applies: a trainer gets only the sessions they teach, which
 * the tab says rather than passing off as the student's whole timetable.
 */
export function StudentScheduleTab({ enrollments }) {
  const { user } = useAuth()
  const courseIds = [
    ...new Set(
      enrollments
        .filter((enrollment) => enrollment.status === 'APPROVED' || enrollment.status === 'COMPLETED')
        .map((enrollment) => enrollment.course.id),
    ),
  ]
  const results = useSessionsForCoursesQueries(courseIds, { size: SESSIONS_PER_COURSE, sort: 'sessionDate,asc' })

  if (courseIds.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        This student isn’t approved on any course yet, so there is nothing scheduled for them.
      </p>
    )
  }

  if (results.some((result) => result.isLoading)) {
    return <p className="text-sm text-muted-foreground">Loading…</p>
  }

  const today = todayIsoDate()
  const byStart = (a, b) =>
    a.sessionDate.localeCompare(b.sessionDate) || a.startTime.localeCompare(b.startTime)
  const sessions = results.flatMap((result) => result.data?.content ?? []).sort(byStart)
  const upcoming = sessions.filter((session) => session.sessionDate >= today)
  const past = sessions.filter((session) => session.sessionDate < today).reverse()
  const failed = results.some((result) => result.isError)

  return (
    <div className="space-y-6">
      {user.role === 'TRAINER' && (
        <p className="text-sm text-muted-foreground">Only the sessions you teach are listed.</p>
      )}
      {failed && <p className="text-sm text-destructive">Some of this student’s courses couldn’t be loaded.</p>}

      <section className="space-y-2">
        <h2 className="text-sm font-semibold">Upcoming</h2>
        <SessionsTable sessions={upcoming} emptyMessage="Nothing scheduled from today on." />
      </section>

      <section className="space-y-2">
        <h2 className="text-sm font-semibold">Past</h2>
        <SessionsTable sessions={past} emptyMessage="No sessions have taken place yet." />
      </section>
    </div>
  )
}
