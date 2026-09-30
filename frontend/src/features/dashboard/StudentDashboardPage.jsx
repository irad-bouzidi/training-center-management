import { useTranslation } from 'react-i18next'
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
  const { t } = useTranslation('dashboard')
  const { user } = useAuth()
  const { data: summary, isLoading } = useStudentSummaryQuery(user.id)

  const approved = summary?.enrollments.filter((enrollment) => enrollment.status === 'APPROVED').length ?? 0

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('welcome', { name: user.name })}</CardTitle>
        <CardDescription>{t('student.description')}</CardDescription>
      </CardHeader>

      <CardContent>
        {isLoading && <p className="text-sm text-muted-foreground">{t('common:states.loading')}</p>}

        {summary && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatTile
              label={t('student.activeEnrollments')}
              value={formatCount(approved)}
              note={t('student.totalEnrollments', { total: formatCount(summary.enrollments.length) })}
              to="/student/enrollments"
            />
            <StatTile
              label={t('student.attendance')}
              value={formatRate(summary.attendanceRate)}
              note={t('student.acrossMarkedSessions')}
            />
            <StatTile
              label={t('student.overallGrade')}
              value={formatPercent(summary.overallGrade)}
              to="/student/grades"
            />
            <StatTile
              label={t('student.balanceOwed')}
              value={formatAmount(summary.paymentBalance ?? 0)}
              note={Number(summary.paymentBalance) > 0 ? t('student.stillToPay') : t('student.nothingOutstanding')}
              tone={Number(summary.paymentBalance) > 0 ? 'attention' : 'default'}
              to="/student/payments"
            />
            <StatTile
              label={t('student.certificates')}
              value={formatCount(summary.certificates.length)}
              to="/student/certificates"
            />
          </div>
        )}
      </CardContent>
    </Card>
  )
}
