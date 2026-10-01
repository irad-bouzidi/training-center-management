import { useTranslation } from 'react-i18next'
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
  const { t } = useTranslation('dashboard')
  const { user } = useAuth()
  const { data: summary, isLoading } = useAdminSummaryQuery()

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>{t('welcome', { name: user.name })}</CardTitle>
          <CardDescription>{t('admin.description')}</CardDescription>
        </CardHeader>

        <CardContent className="space-y-6">
          {isLoading && <p className="text-sm text-muted-foreground">{t('common:states.loading')}</p>}

          {summary && (
            <>
              <section className="space-y-2">
                <h2 className="text-sm font-semibold">{t('admin.sections.people')}</h2>
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  <StatTile
                    label={t('admin.activeStudents')}
                    value={formatCount(summary.activeStudents)}
                    to="/admin/students"
                  />
                  <StatTile
                    label={t('admin.activeTrainers')}
                    value={formatCount(summary.activeTrainers)}
                    to="/admin/users"
                  />
                  <StatTile
                    label={t('admin.publishedCourses')}
                    value={formatCount(summary.publishedCourses)}
                    to="/admin/courses"
                  />
                  <StatTile
                    label={t('admin.pendingEnrollments')}
                    value={formatCount(summary.pendingEnrollments)}
                    note={summary.pendingEnrollments > 0 ? t('admin.pendingWaiting') : t('admin.pendingNone')}
                    tone={summary.pendingEnrollments > 0 ? 'attention' : 'default'}
                    to="/admin/enrollments"
                  />
                </div>
              </section>

              <section className="space-y-2">
                <h2 className="text-sm font-semibold">{t('admin.sections.teaching')}</h2>
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  <StatTile
                    label={t('admin.upcomingSessions')}
                    value={formatCount(summary.upcomingSessions)}
                    note={t('admin.next7Days')}
                    to="/admin/schedule"
                  />
                  <StatTile
                    label={t('admin.averageAttendance')}
                    value={formatRate(summary.averageAttendanceRate)}
                    note={t('admin.acrossMarkedSessions')}
                  />
                  <StatTile
                    label={t('admin.certificatesIssued')}
                    value={formatCount(summary.certificatesThisMonth)}
                    note={t('admin.thisMonth')}
                  />
                </div>
              </section>

              <section className="space-y-2">
                <h2 className="text-sm font-semibold">{t('admin.sections.money')}</h2>
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  <StatTile
                    label={t('admin.outstandingBalance')}
                    value={formatAmount(summary.outstandingBalance)}
                    note={t('admin.owedAcrossInvoices')}
                    to="/admin/payments"
                  />
                  <StatTile
                    label={t('admin.overdueInvoices')}
                    value={formatCount(summary.overdueInvoices)}
                    note={summary.overdueInvoices > 0 ? t('admin.overduePast') : t('admin.overdueNone')}
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
