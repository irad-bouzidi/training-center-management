import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/context/AuthContext'
import { courseDetailPathForRole } from '@/lib/roleHomePaths'
import { formatPrice } from './courseDisplay'
import { useCoursesQuery } from './hooks'

const PAGE_SIZE = 12

/**
 * Read-only catalog shared by Trainer/Student, card grid, published-only -
 * see docs/tasks/TCM-12-frontend-course-management.md step 5. The backend
 * already restricts non-admin callers to PUBLISHED courses regardless of any
 * status filter (see CourseController#search), so no client-side status
 * filtering is needed here. The enroll action lands in TCM-16.
 */
export function CourseCatalogPage() {
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
  const courses = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-lg font-semibold">Course Catalog</h1>
        <Input
          placeholder="Search courses…"
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
          className="max-w-64"
        />
      </div>

      {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
      {!isLoading && courses.length === 0 && (
        <p className="text-sm text-muted-foreground">No published courses found.</p>
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
                {course.description || 'No description provided.'}
              </p>
              <div className="flex flex-wrap gap-2">
                {course.category && <Badge variant="outline">{course.category}</Badge>}
                <Badge variant="outline">{course.durationHours}h</Badge>
              </div>
            </CardContent>
            <CardFooter className="flex items-center justify-between text-sm text-muted-foreground">
              <span>{course.primaryTrainer?.name ?? 'Unassigned'}</span>
              <span className="font-medium text-foreground">{formatPrice(course.price)}</span>
            </CardFooter>
          </Card>
        ))}
      </div>

      <div className="flex items-center justify-between text-sm text-muted-foreground">
        <p>{totalElements} course{totalElements === 1 ? '' : 's'}</p>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>
            Previous
          </Button>
          <span>
            Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((current) => current + 1)}
          >
            Next
          </Button>
        </div>
      </div>
    </div>
  )
}
