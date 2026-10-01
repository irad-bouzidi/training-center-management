import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/context/AuthContext'
import { EnrollButton } from '@/features/enrollments/EnrollButton'
import { useMyEnrollmentsByCourseQuery } from '@/features/enrollments/hooks'
import { courseDetailPathForRole } from '@/lib/roleHomePaths'
import { formatPrice } from './courseDisplay'
import { useCoursesQuery } from './hooks'

const PAGE_SIZE = 12

/**
 * Read-only catalog shared by Trainer/Student, card grid, published-only -
 * see docs/tasks/TCM-12-frontend-course-management.md step 5. The backend
 * already restricts non-admin callers to PUBLISHED courses regardless of any
 * status filter (see CourseController#search), so no client-side status
 * filtering is needed here. A Student additionally gets the enroll action on
 * each card, per docs/tasks/TCM-16-frontend-course-catalog-enrollment.md
 * step 2; a Trainer sees the same grid read-only.
 */
export function CourseCatalogPage() {
  const { t } = useTranslation('courses')
  const { user } = useAuth()
  const navigate = useNavigate()

  const [searchInput, setSearchInput] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)

  useEffect(() => {
    const handle = setTimeout(() => {
      setQuery(searchInput.trim())
      setPage(0)
    }, 300)
    return () => clearTimeout(handle)
  }, [searchInput])

  const { data, isLoading } = useCoursesQuery({ page, size: PAGE_SIZE, query: query || undefined })
  const isStudent = user.role === 'STUDENT'
  const { data: myEnrollments } = useMyEnrollmentsByCourseQuery(isStudent)
  const courses = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-lg font-semibold">{t('catalog.title')}</h1>
        <Input
          placeholder={t('catalog.searchPlaceholder')}
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
          className="max-w-64"
        />
      </div>

      {isLoading && <p className="text-sm text-muted-foreground">{t('common:states.loading')}</p>}
      {!isLoading && courses.length === 0 && (
        <p className="text-sm text-muted-foreground">{t('catalog.empty')}</p>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {courses.map((course) => (
          <Card
            key={course.id}
            role="button"
            tabIndex={0}
            className="cursor-pointer transition-colors hover:bg-accent/50"
            onClick={() => navigate(courseDetailPathForRole(user.role, course.id))}
            onKeyDown={(event) => {
              if (event.key === 'Enter') navigate(courseDetailPathForRole(user.role, course.id))
            }}
          >
            <CardHeader>
              <CardTitle>{course.name}</CardTitle>
              <CardDescription>{course.code}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2">
              <p className="line-clamp-2 text-sm text-muted-foreground">
                {course.description || t('shared.noDescription')}
              </p>
              <div className="flex flex-wrap gap-2">
                {course.category && <Badge variant="outline">{course.category}</Badge>}
                <Badge variant="outline">{t('shared.hours', { count: course.durationHours })}</Badge>
                <Badge variant="outline">
                  {t('catalog.enrolled', { approved: course.approvedCount, capacity: course.capacity })}
                </Badge>
              </div>
            </CardContent>
            <CardFooter className="flex items-center justify-between gap-2 text-sm text-muted-foreground">
              <span className="truncate">{course.primaryTrainer?.name ?? t('shared.unassigned')}</span>
              <div className="flex shrink-0 items-center gap-3">
                <span className="font-medium text-foreground">{formatPrice(course.price)}</span>
                {isStudent && <EnrollButton course={course} enrollment={myEnrollments?.get(course.id)} />}
              </div>
            </CardFooter>
          </Card>
        ))}
      </div>

      <div className="flex items-center justify-between text-sm text-muted-foreground">
        <p>{t('shared.count', { count: totalElements })}</p>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>
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
    </div>
  )
}
