import { useTranslation } from 'react-i18next'
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
  const { t } = useTranslation('dashboard')
  const { user } = useAuth()
  const { data: summary, isLoading } = useTrainerSummaryQuery()

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('welcome', { name: user.name })}</CardTitle>
        <CardDescription>{t('trainer.description')}</CardDescription>
      </CardHeader>

      <CardContent>
        {isLoading && <p className="text-sm text-muted-foreground">{t('common:states.loading')}</p>}

        {summary && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatTile label={t('trainer.myCourses')} value={formatCount(summary.myCourses)} to="/trainer/my-courses" />
            <StatTile
              label={t('trainer.upcomingSessions')}
              value={formatCount(summary.upcomingSessions)}
              note={t('trainer.next7Days')}
              to="/trainer/schedule"
            />
            <StatTile
              label={t('trainer.sessionsToMark')}
              value={formatCount(summary.sessionsAwaitingAttendance)}
              note={
                summary.sessionsAwaitingAttendance > 0
                  ? t('trainer.sessionsToMarkPending')
                  : t('trainer.sessionsToMarkNone')
              }
              tone={summary.sessionsAwaitingAttendance > 0 ? 'attention' : 'default'}
              to="/trainer/schedule"
            />
            <StatTile
              label={t('trainer.studentsToGrade')}
              value={formatCount(summary.studentsAwaitingGrades)}
              note={
                summary.studentsAwaitingGrades > 0 ? t('trainer.studentsToGradePending') : t('trainer.studentsToGradeNone')
              }
              tone={summary.studentsAwaitingGrades > 0 ? 'attention' : 'default'}
              to="/trainer/my-courses"
            />
          </div>
        )}
      </CardContent>
    </Card>
  )
}
