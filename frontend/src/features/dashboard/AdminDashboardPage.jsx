import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuth } from '@/context/AuthContext'
import { formatRate } from '@/features/attendance/attendanceDisplay'
import { formatAmount } from '@/features/payments/paymentDisplay'
import { StatTile } from './StatTile'
import { formatCount } from './dashboardDisplay'
import { useAdminSummaryQuery } from './hooks'

/**
 * The administrator's home (/admin), per docs/tasks/TCM-29 step 3: live
 * counts across every domain, each tile a way in to the section it's about.
 *
 * Every figure is read straight from GET /dashboard/summary - nothing is
 * derived here, so what's on screen is what the database says.
 */
export function AdminDashboardPage() {
  const { user } = useAuth()
  const { data: summary, isLoading } = useAdminSummaryQuery()

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Welcome, {user.name}</CardTitle>
          <CardDescription>What’s happening across the centre right now.</CardDescription>
        </CardHeader>

        <CardContent className="space-y-6">
          {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}

          {summary && (
            <>
              <section className="space-y-2">
                <h2 className="text-sm font-semibold">People and courses</h2>
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  <StatTile label="Active students" value={formatCount(summary.activeStudents)} to="/admin/students" />
                  <StatTile label="Active trainers" value={formatCount(summary.activeTrainers)} to="/admin/users" />
                  <StatTile
                    label="Published courses"
                    value={formatCount(summary.publishedCourses)}
                    to="/admin/courses"
                  />
                  <StatTile
                    label="Pending enrollments"
                    value={formatCount(summary.pendingEnrollments)}
                    note={summary.pendingEnrollments > 0 ? 'Waiting on a decision' : 'Nothing waiting'}
                    tone={summary.pendingEnrollments > 0 ? 'attention' : 'default'}
                    to="/admin/enrollments"
                  />
                </div>
              </section>

              <section className="space-y-2">
                <h2 className="text-sm font-semibold">Teaching</h2>
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  <StatTile
                    label="Upcoming sessions"
                    value={formatCount(summary.upcomingSessions)}
                    note="Next 7 days"
                    to="/admin/schedule"
                  />
                  <StatTile
                    label="Average attendance"
                    value={formatRate(summary.averageAttendanceRate)}
                    note="Across every marked session"
                  />
                  <StatTile
                    label="Certificates issued"
                    value={formatCount(summary.certificatesThisMonth)}
                    note="This month"
                  />
                </div>
              </section>

              <section className="space-y-2">
                <h2 className="text-sm font-semibold">Money</h2>
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  <StatTile
                    label="Outstanding balance"
                    value={formatAmount(summary.outstandingBalance)}
                    note="Owed across all invoices"
                    to="/admin/payments"
                  />
                  <StatTile
                    label="Overdue invoices"
                    value={formatCount(summary.overdueInvoices)}
                    note={summary.overdueInvoices > 0 ? 'Past their due date' : 'None past due'}
                    tone={summary.overdueInvoices > 0 ? 'attention' : 'default'}
                    to="/admin/payments"
                  />
                </div>
              </section>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
