import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuth } from '@/context/AuthContext'
import { StatTile } from './StatTile'
import { formatCount } from './dashboardDisplay'
import { useTrainerSummaryQuery } from './hooks'

/**
 * The trainer's home (/trainer), per docs/tasks/TCM-29 step 3 - the same
 * kind of answer as the admin's, scoped to what they teach. The two
 * "awaiting" tiles are the ones worth acting on, so they carry the
 * attention tone when they aren't zero.
 */
export function TrainerDashboardPage() {
  const { user } = useAuth()
  const { data: summary, isLoading } = useTrainerSummaryQuery()

  return (
    <Card>
      <CardHeader>
        <CardTitle>Welcome, {user.name}</CardTitle>
        <CardDescription>Your courses, and what’s waiting on you.</CardDescription>
      </CardHeader>

      <CardContent>
        {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}

        {summary && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatTile label="My courses" value={formatCount(summary.myCourses)} to="/trainer/courses" />
            <StatTile
              label="Upcoming sessions"
              value={formatCount(summary.upcomingSessions)}
              note="Next 7 days"
              to="/trainer/schedule"
            />
            <StatTile
              label="Sessions to mark"
              value={formatCount(summary.sessionsAwaitingAttendance)}
              note={
                summary.sessionsAwaitingAttendance > 0
                  ? 'Delivered with no attendance recorded'
                  : 'Attendance is up to date'
              }
              tone={summary.sessionsAwaitingAttendance > 0 ? 'attention' : 'default'}
              to="/trainer/schedule"
            />
            <StatTile
              label="Students to grade"
              value={formatCount(summary.studentsAwaitingGrades)}
              note={
                summary.studentsAwaitingGrades > 0 ? 'Nothing recorded for them yet' : 'Everyone has a grade'
              }
              tone={summary.studentsAwaitingGrades > 0 ? 'attention' : 'default'}
              to="/trainer/courses"
            />
          </div>
        )}
      </CardContent>
    </Card>
  )
}
