import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useAuth } from '@/context/AuthContext'
import { useCoursesQuery, useTrainersQuery } from '@/features/courses/hooks'
import { ScheduleFormDialog } from './ScheduleFormDialog'
import { SessionRowActions } from './SessionRowActions'
import {
  formatSessionDate,
  formatTimeRange,
  groupByDate,
  statusBadgeVariant,
  todayIsoDate,
} from './scheduleDisplay'
import { useSessionsQuery } from './hooks'

// An agenda is read a screen at a time rather than paged like a table, so one
// page holds a good stretch of it.
const PAGE_SIZE = 50
const PICKER_SIZE = 200
const ALL = 'ALL'

/**
 * The schedule agenda, shared by all three roles and mounted at each one's own
 * `<home>/schedule` - see docs/tasks/TCM-18-frontend-scheduling.md step 3.
 * Scoping is the backend's (ClassSessionController#search): an ADMIN sees
 * every session, a TRAINER only their own, a STUDENT only sessions of courses
 * they're approved in. So this page asks for the same thing for everyone and
 * varies only in what it offers to do with the answer - an ADMIN gets the
 * course/trainer filters and the scheduling actions, the other two roles read.
 *
 * Sessions come back as a flat page and are grouped into days here, which is
 * the "week/list agenda" the task asks for; a calendar grid is explicitly
 * optional.
 */
export function ScheduleListPage() {
  const { t } = useTranslation('schedule')
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'

  // Opens on what's still to come - the common question of a schedule - while
  // leaving the field clearable to look back.
  const [from, setFrom] = useState(todayIsoDate)
  const [to, setTo] = useState('')
  const [courseId, setCourseId] = useState(ALL)
  const [trainerId, setTrainerId] = useState(ALL)
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)

  const { data: coursesPage } = useCoursesQuery({ size: PICKER_SIZE }, { enabled: isAdmin })
  const { data: trainers } = useTrainersQuery({ enabled: isAdmin })

  const { data, isLoading } = useSessionsQuery({
    page,
    size: PAGE_SIZE,
    sort: 'sessionDate,asc',
    from: from || undefined,
    to: to || undefined,
    courseId: isAdmin && courseId !== ALL ? courseId : undefined,
    trainerId: isAdmin && trainerId !== ALL ? trainerId : undefined,
  })

  const days = groupByDate(data?.content ?? [])
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  // Any filter change can shrink the result set, so never leave the viewer
  // stranded on a page that no longer exists.
  function updateFilter(setter, value) {
    setter(value)
    setPage(0)
  }

  function openCreate() {
    setEditing(null)
    setFormOpen(true)
  }

  function openEdit(session) {
    setEditing(session)
    setFormOpen(true)
  }

  return (
    <>
      <Card>
        <CardHeader className="flex-row items-center justify-between">
          <CardTitle>{t('list.title')}</CardTitle>
          {isAdmin && <Button onClick={openCreate}>{t('newSession')}</Button>}
        </CardHeader>

        <CardContent className="space-y-4">
          <div className="flex flex-wrap items-end gap-3">
            <div className="space-y-2">
              <Label htmlFor="from">{t('list.from')}</Label>
              <Input
                id="from"
                type="date"
                className="w-40"
                value={from}
                onChange={(event) => updateFilter(setFrom, event.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="to">{t('list.to')}</Label>
              <Input
                id="to"
                type="date"
                className="w-40"
                value={to}
                onChange={(event) => updateFilter(setTo, event.target.value)}
              />
            </div>

            {isAdmin && (
              <>
                <div className="space-y-2">
                  <Label htmlFor="courseFilter">{t('list.course')}</Label>
                  <Select value={courseId} onValueChange={(value) => updateFilter(setCourseId, value)}>
                    <SelectTrigger id="courseFilter" className="w-56">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={ALL}>{t('list.allCourses')}</SelectItem>
                      {coursesPage?.content?.map((course) => (
                        <SelectItem key={course.id} value={course.id}>
                          {course.name} ({course.code})
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="trainerFilter">{t('list.trainer')}</Label>
                  <Select value={trainerId} onValueChange={(value) => updateFilter(setTrainerId, value)}>
                    <SelectTrigger id="trainerFilter" className="w-48">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={ALL}>{t('list.allTrainers')}</SelectItem>
                      {trainers?.map((trainer) => (
                        <SelectItem key={trainer.id} value={trainer.id}>
                          {trainer.firstName} {trainer.lastName}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </>
            )}
          </div>

          {isLoading && <p className="text-sm text-muted-foreground">{t('common:states.loading')}</p>}
          {!isLoading && days.length === 0 && (
            <p className="text-sm text-muted-foreground">{t('list.empty')}</p>
          )}

          <div className="space-y-6">
            {days.map(({ date, sessions }) => (
              <section key={date} className="space-y-2">
                <h2 className="text-sm font-semibold">{formatSessionDate(date)}</h2>
                <ul className="divide-y rounded-md border">
                  {sessions.map((session) => (
                    <li key={session.id} className="flex flex-wrap items-center gap-3 px-4 py-3">
                      <span className="w-28 shrink-0 font-mono text-sm">
                        {formatTimeRange(session.startTime, session.endTime)}
                      </span>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium">
                          {session.course.name}{' '}
                          <span className="text-xs font-normal text-muted-foreground">{session.course.code}</span>
                        </p>
                        <p className="truncate text-xs text-muted-foreground">
                          {session.classroom} · {session.trainer.name}
                        </p>
                      </div>
                      <Badge variant={statusBadgeVariant(session.status)}>{t(`common:enums.sessionStatus.${session.status}`)}</Badge>
                      <SessionRowActions session={session} onEdit={openEdit} />
                    </li>
                  ))}
                </ul>
              </section>
            ))}
          </div>

          <div className="flex items-center justify-between text-sm text-muted-foreground">
            <p>{t('list.count', { count: totalElements })}</p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((current) => current - 1)}
              >
                {t('common:actions.previous')}
              </Button>
              <span>{t('common:pagination.pageOf', { page: totalPages === 0 ? 0 : page + 1, total: totalPages })}</span>
              <Button
                variant="outline"
                size="sm"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((current) => current + 1)}
              >
                {t('common:actions.next')}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {isAdmin && <ScheduleFormDialog open={formOpen} onOpenChange={setFormOpen} session={editing} />}
    </>
  )
}
