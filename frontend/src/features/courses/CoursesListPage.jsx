import { Plus } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { CourseFormDialog } from './CourseFormDialog'
import { CourseRowActions } from './CourseRowActions'
import { formatPrice, STATUS_OPTIONS, statusBadgeVariant, titleCase } from './courseDisplay'
import { useCoursesQuery } from './hooks'

const PAGE_SIZE = 20
const ALL = 'ALL'

export function CoursesListPage() {
  const navigate = useNavigate()

  const [status, setStatus] = useState(ALL)
  const [categoryInput, setCategoryInput] = useState('')
  const [category, setCategory] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)

  const [formDialog, setFormDialog] = useState(null) // null | { course: null | object }

  // Debounce both free-text filters so every keystroke doesn't fire a
  // request. Once a debounced value actually changes the result set, hop
  // back to page 0 rather than staying on a possibly out-of-range page.
  useEffect(() => {
    const handle = setTimeout(() => {
      setQuery(searchInput.trim())
      setPage(0)
    }, 300)
    return () => clearTimeout(handle)
  }, [searchInput])

  useEffect(() => {
    const handle = setTimeout(() => {
      setCategory(categoryInput.trim())
      setPage(0)
    }, 300)
    return () => clearTimeout(handle)
  }, [categoryInput])

  function handleStatusChange(value) {
    setStatus(value)
    setPage(0)
  }

  const { data, isLoading } = useCoursesQuery({
    page,
    size: PAGE_SIZE,
    status: status === ALL ? undefined : status,
    category: category || undefined,
    query: query || undefined,
  })

  const courses = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between">
        <CardTitle>Courses</CardTitle>
        <Button onClick={() => setFormDialog({ course: null })}>
          <Plus />
          New Course
        </Button>
      </CardHeader>

      <CardContent className="space-y-4">
        <div className="flex flex-wrap items-center gap-2">
          <Input
            placeholder="Search by name or code…"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            className="max-w-56"
          />

          <Input
            placeholder="Category…"
            value={categoryInput}
            onChange={(event) => setCategoryInput(event.target.value)}
            className="max-w-40"
          />

          <Select value={status} onValueChange={handleStatusChange}>
            <SelectTrigger className="w-36">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All statuses</SelectItem>
              {STATUS_OPTIONS.map((option) => (
                <SelectItem key={option} value={option}>
                  {titleCase(option)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Code</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Trainer</TableHead>
              <TableHead>Capacity</TableHead>
              <TableHead>Price</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="w-10" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground">
                  Loading…
                </TableCell>
              </TableRow>
            )}

            {!isLoading && courses.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground">
                  No courses found.
                </TableCell>
              </TableRow>
            )}

            {courses.map((course) => (
              <TableRow key={course.id}>
                <TableCell className="font-mono text-xs">{course.code}</TableCell>
                <TableCell>{course.name}</TableCell>
                <TableCell>{course.primaryTrainer?.name ?? '—'}</TableCell>
                <TableCell>{course.capacity}</TableCell>
                <TableCell>{formatPrice(course.price)}</TableCell>
                <TableCell>
                  <Badge variant={statusBadgeVariant(course.status)}>{titleCase(course.status)}</Badge>
                </TableCell>
                <TableCell>
                  <CourseRowActions
                    course={course}
                    onView={(c) => navigate(`/admin/courses/${c.id}`)}
                    onEdit={(c) => setFormDialog({ course: c })}
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>

        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <p>{totalElements} course{totalElements === 1 ? '' : 's'}</p>
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

      {formDialog && (
        <CourseFormDialog
          key={formDialog.course?.id ?? 'new'}
          open={Boolean(formDialog)}
          onOpenChange={(next) => !next && setFormDialog(null)}
          course={formDialog.course}
        />
      )}
    </Card>
  )
}
