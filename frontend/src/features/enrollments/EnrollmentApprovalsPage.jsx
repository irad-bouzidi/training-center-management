import { useState } from 'react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { EnrollmentRowActions } from './EnrollmentRowActions'
import { formatDate, STATUS_OPTIONS, statusBadgeVariant, titleCase } from './enrollmentDisplay'
import { useEnrollmentsQuery } from './hooks'

const PAGE_SIZE = 20
const ALL = 'ALL'

/**
 * The Admin's enrollment queue, per
 * docs/tasks/TCM-16-frontend-course-catalog-enrollment.md step 4. Defaults to
 * the PENDING filter - the requests actually waiting on a decision - with the
 * other statuses (and "All") available for looking back at what was decided.
 */
export function EnrollmentApprovalsPage() {
  const [status, setStatus] = useState('PENDING')
  const [page, setPage] = useState(0)

  function handleStatusChange(value) {
    setStatus(value)
    setPage(0)
  }

  const { data, isLoading } = useEnrollmentsQuery({
    page,
    size: PAGE_SIZE,
    sort: 'enrolledAt,desc',
    status: status === ALL ? undefined : status,
  })

  const enrollments = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  return (
    <Card>
      <CardHeader>
        <CardTitle>Enrollments</CardTitle>
      </CardHeader>

      <CardContent className="space-y-4">
        <Select value={status} onValueChange={handleStatusChange}>
          <SelectTrigger className="w-40">
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

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Student</TableHead>
              <TableHead>Course</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Requested</TableHead>
              <TableHead>Decided By</TableHead>
              <TableHead className="w-44" />
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

            {!isLoading && enrollments.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  {status === 'PENDING' ? 'No enrollment requests waiting for a decision.' : 'No enrollments found.'}
                </TableCell>
              </TableRow>
            )}

            {enrollments.map((enrollment) => (
              <TableRow key={enrollment.id}>
                <TableCell>
                  {enrollment.student.name}{' '}
                  <span className="text-xs text-muted-foreground">{enrollment.student.email}</span>
                </TableCell>
                <TableCell>
                  {enrollment.course.name}{' '}
                  <span className="text-xs text-muted-foreground">{enrollment.course.code}</span>
                </TableCell>
                <TableCell>
                  <Badge variant={statusBadgeVariant(enrollment.status)}>{titleCase(enrollment.status)}</Badge>
                </TableCell>
                <TableCell>{formatDate(enrollment.enrolledAt)}</TableCell>
                <TableCell>{enrollment.decidedBy?.name ?? '—'}</TableCell>
                <TableCell>
                  <EnrollmentRowActions enrollment={enrollment} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>

        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <p>{totalElements} enrollment{totalElements === 1 ? '' : 's'}</p>
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
