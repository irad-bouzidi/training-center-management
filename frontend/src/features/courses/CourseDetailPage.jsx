import { ArrowLeft, BookOpenCheck } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useAuth } from '@/context/AuthContext'
import { CourseAttendanceReport } from '@/features/attendance/CourseAttendanceReport'
import { CourseEnrollmentsTab } from '@/features/enrollments/CourseEnrollmentsTab'
import { CourseScheduleTab } from '@/features/schedule/CourseScheduleTab'
import { CourseFormDialog } from './CourseFormDialog'
import { CourseRowActions } from './CourseRowActions'
import { formatDate, formatPrice, statusBadgeVariant } from './courseDisplay'
import { useCourseQuery } from './hooks'

function Field({ label, value }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm">{value}</p>
    </div>
  )
}

/**
 * Full course detail view, mounted under every role's own layout (e.g.
 * /admin/courses/:id, /trainer/courses/:id, /student/courses/:id) - see
 * docs/tasks/TCM-12-frontend-course-management.md step 4. Only ADMIN gets
 * the edit/status actions; Trainer/Student reach this same route read-only,
 * from the shared catalog (CourseCatalogPage). The tabs are role-aware: see
 * CourseScheduleTab (TCM-18), CourseEnrollmentsTab (TCM-16) and
 * CourseAttendanceReport (TCM-20).
 */
export function CourseDetailPage() {
  const { t } = useTranslation('courses')
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { data: course, isLoading } = useCourseQuery(id)
  const [formOpen, setFormOpen] = useState(false)
  const isAdmin = user.role === 'ADMIN'
  // Students never see anyone's attendance or gradebook but their own (TCM-19
  // and TCM-23 grant both to admins and the course's trainers only), so
  // neither is offered to them at all rather than shown and then refused.
  const isStaff = user.role !== 'STUDENT'

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">{t('common:states.loading')}</p>
  }

  if (!course) {
    return <p className="text-sm text-muted-foreground">{t('detail.notFound')}</p>
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
            <CardTitle className="flex items-center gap-2">
              {course.name}
              <Badge variant={statusBadgeVariant(course.status)}>{t(`common:enums.courseStatus.${course.status}`)}</Badge>
            </CardTitle>
            <CardDescription>{course.code}</CardDescription>
          </div>
          <div className="flex items-center gap-2">
            {isStaff && (
              <Button variant="outline" size="sm" asChild>
                <Link to={`/${user.role.toLowerCase()}/courses/${course.id}/grades`}>
                  <BookOpenCheck />
                  {t('shared.gradebook')}
                </Link>
              </Button>
            )}
            {isAdmin && <CourseRowActions course={course} onEdit={() => setFormOpen(true)} />}
          </div>
        </CardHeader>
        <CardContent className="space-y-6">
          <p className="text-sm text-muted-foreground">{course.description || t('shared.noDescription')}</p>

          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <Field label={t('detail.fields.trainer')} value={course.primaryTrainer?.name ?? t('shared.unassigned')} />
            <Field label={t('detail.fields.category')} value={course.category || '—'} />
            <Field label={t('detail.fields.duration')} value={t('shared.hours', { count: course.durationHours })} />
            <Field label={t('detail.fields.capacity')} value={course.capacity} />
            <Field label={t('detail.fields.price')} value={formatPrice(course.price)} />
            <Field label={t('detail.fields.created')} value={formatDate(course.createdAt)} />
          </div>

          <Tabs defaultValue="schedule">
            <TabsList>
              <TabsTrigger value="schedule">{t('detail.tabs.schedule')}</TabsTrigger>
              <TabsTrigger value="enrollments">{t('detail.tabs.enrollments')}</TabsTrigger>
              {isStaff && <TabsTrigger value="attendance">{t('detail.tabs.attendance')}</TabsTrigger>}
            </TabsList>
            <TabsContent value="schedule">
              <CourseScheduleTab course={course} />
            </TabsContent>
            <TabsContent value="enrollments">
              <CourseEnrollmentsTab course={course} />
            </TabsContent>
            {isStaff && (
              <TabsContent value="attendance">
                <CourseAttendanceReport courseId={course.id} />
              </TabsContent>
            )}
          </Tabs>
        </CardContent>
      </Card>

      {isAdmin && <CourseFormDialog open={formOpen} onOpenChange={setFormOpen} course={course} />}
    </div>
  )
}
