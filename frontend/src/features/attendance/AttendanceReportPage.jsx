import { ArrowLeft } from 'lucide-react'
import { useTranslation } from 'react-i18next'
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
  const { t } = useTranslation('attendance')
  const { courseId } = useParams()
  const navigate = useNavigate()
  const { data: course } = useCourseQuery(courseId)

  return (
    <div className="space-y-4">
      <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
        <ArrowLeft />
        {t('common:actions.back')}
      </Button>

      <Card>
        <CardHeader>
          <CardTitle>{t('reportPage.title')}</CardTitle>
          <CardDescription>{course
              ? t('reportPage.courseLabel', { name: course.name, code: course.code })
              : t('reportPage.fallbackDescription')}</CardDescription>
        </CardHeader>
        <CardContent>
          <CourseAttendanceReport courseId={courseId} />
        </CardContent>
      </Card>
    </div>
  )
}
