import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuth } from '@/context/AuthContext'
import { formatRate } from '@/features/attendance/attendanceDisplay'
import { formatPercent } from '@/features/grades/gradeDisplay'
import { formatAmount } from '@/features/payments/paymentDisplay'
import { useStudentSummaryQuery } from '@/features/students/hooks'
import { StatTile } from './StatTile'
import { formatCount } from './dashboardDisplay'

/**
 * The student's home (/student), per docs/tasks/TCM-29 step 3. Composed from
 * their own summary (TCM-13, which a student may read for themselves) rather
 * than a dashboard endpoint of its own - every figure it needs is already
 * there.
 */
export function StudentDashboardPage() {
  const { user } = useAuth()
  const { data: summary, isLoading } = useStudentSummaryQuery(user.id)

  const approved = summary?.enrollments.filter((enrollment) => enrollment.status === 'APPROVED').length ?? 0

  return (
    <Card>
      <CardHeader>
        <CardTitle>Welcome, {user.name}</CardTitle>
        <CardDescription>Where you stand across your courses.</CardDescription>
      </CardHeader>

      <CardContent>
        {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}

        {summary && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatTile
              label="Active enrollments"
              value={formatCount(approved)}
              note={`${summary.enrollments.length} in total`}
              to="/student/enrollments"
            />
            <StatTile
              label="Attendance"
              value={formatRate(summary.attendanceRate)}
              note="Across every marked session"
            />
            <StatTile label="Overall grade" value={formatPercent(summary.overallGrade)} to="/student/grades" />
            <StatTile
              label="Balance owed"
              value={formatAmount(summary.paymentBalance ?? 0)}
              note={Number(summary.paymentBalance) > 0 ? 'Still to pay' : 'Nothing outstanding'}
              tone={Number(summary.paymentBalance) > 0 ? 'attention' : 'default'}
              to="/student/payments"
            />
            <StatTile
              label="Certificates"
              value={formatCount(summary.certificates.length)}
              to="/student/certificates"
            />
          </div>
        )}
      </CardContent>
    </Card>
  )
}
