import { ArrowLeft } from 'lucide-react'
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
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
import { formatDate, formatPrice, statusBadgeVariant, titleCase } from './courseDisplay'
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
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { data: course, isLoading } = useCourseQuery(id)
  const [formOpen, setFormOpen] = useState(false)
  const isAdmin = user.role === 'ADMIN'
  // Students never see anyone's attendance but their own (TCM-19 grants the
  // report to admins and the course's trainers only), so the tab isn't
  // offered to them at all rather than shown and then refused.
  const canSeeAttendance = user.role !== 'STUDENT'

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">Loading…</p>
  }

  if (!course) {
    return <p className="text-sm text-muted-foreground">Course not found.</p>
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
            <CardTitle className="flex items-center gap-2">
              {course.name}
              <Badge variant={statusBadgeVariant(course.status)}>{titleCase(course.status)}</Badge>
            </CardTitle>
            <CardDescription>{course.code}</CardDescription>
          </div>
          {isAdmin && <CourseRowActions course={course} onEdit={() => setFormOpen(true)} />}
        </CardHeader>
        <CardContent className="space-y-6">
          <p className="text-sm text-muted-foreground">{course.description || 'No description provided.'}</p>

          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <Field label="Trainer" value={course.primaryTrainer?.name ?? 'Unassigned'} />
            <Field label="Category" value={course.category || '—'} />
            <Field label="Duration" value={`${course.durationHours}h`} />
            <Field label="Capacity" value={course.capacity} />
            <Field label="Price" value={formatPrice(course.price)} />
            <Field label="Created" value={formatDate(course.createdAt)} />
          </div>

          <Tabs defaultValue="schedule">
            <TabsList>
              <TabsTrigger value="schedule">Schedule</TabsTrigger>
              <TabsTrigger value="enrollments">Enrollments</TabsTrigger>
              {canSeeAttendance && <TabsTrigger value="attendance">Attendance</TabsTrigger>}
            </TabsList>
            <TabsContent value="schedule">
              <CourseScheduleTab course={course} />
            </TabsContent>
            <TabsContent value="enrollments">
              <CourseEnrollmentsTab course={course} />
            </TabsContent>
            {canSeeAttendance && (
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
