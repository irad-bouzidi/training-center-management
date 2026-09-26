import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { statusBadgeVariant, titleCase } from './courseDisplay'
import { useMyCoursesQuery } from './hooks'

const PAGE_SIZE = 20

/**
 * The trainer's own courses (/trainer/my-courses), every status - unlike the
 * catalog, which only lists PUBLISHED ones, a trainer needs to see a DRAFT
 * they've been assigned before it opens, and an ARCHIVED one they still owe
 * grades on. Each row leads to the course's detail page (roster, schedule)
 * and straight to its gradebook.
 */
export function MyCoursesPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading } = useMyCoursesQuery({ page, size: PAGE_SIZE, sort: 'name,asc' })

  const courses = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  return (
    <Card>
      <CardHeader>
        <CardTitle>My Courses</CardTitle>
        <CardDescription>Every course you’re the trainer of, whatever its status.</CardDescription>
      </CardHeader>

      <CardContent className="space-y-4">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Code</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Category</TableHead>
              <TableHead className="text-right">Enrolled</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="w-28" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  Loading…
                </TableCell>
              </TableRow>
            )}

            {!isLoading && courses.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  You haven’t been assigned any courses yet.
                </TableCell>
              </TableRow>
            )}

            {courses.map((course) => (
              <TableRow key={course.id}>
                <TableCell className="font-mono text-xs">{course.code}</TableCell>
                <TableCell>
                  <Link to={`/trainer/courses/${course.id}`} className="font-medium hover:underline">
                    {course.name}
                  </Link>
                </TableCell>
                <TableCell>{course.category || '—'}</TableCell>
                <TableCell className="text-right tabular-nums">
                  {course.approvedCount} / {course.capacity}
                </TableCell>
                <TableCell>
                  <Badge variant={statusBadgeVariant(course.status)}>{titleCase(course.status)}</Badge>
                </TableCell>
                <TableCell className="text-right">
                  <Button asChild variant="outline" size="sm">
                    <Link to={`/trainer/courses/${course.id}/grades`}>Gradebook</Link>
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>

        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <p>
            {totalElements} course{totalElements === 1 ? '' : 's'}
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((current) => current - 1)}
            >
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
      </CardContent>
    </Card>
  )
}
