import { ArrowLeft } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useCourseQuery } from '@/features/courses/hooks'
import { CourseAttendanceReport } from './CourseAttendanceReport'

/**
 * The course attendance report on a page of its own
 * (/admin/courses/:courseId/attendance), for linking to and printing. The
 * same report is a tab on the course detail page; both render
 * CourseAttendanceReport.
 */
export function AttendanceReportPage() {
  const { courseId } = useParams()
  const navigate = useNavigate()
  const { data: course } = useCourseQuery(courseId)

  return (
    <div className="space-y-4">
      <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
        <ArrowLeft />
        Back
      </Button>

      <Card>
        <CardHeader>
          <CardTitle>Attendance</CardTitle>
          <CardDescription>{course ? `${course.name} (${course.code})` : 'Course attendance report'}</CardDescription>
        </CardHeader>
        <CardContent>
          <CourseAttendanceReport courseId={courseId} />
        </CardContent>
      </Card>
    </div>
  )
}
