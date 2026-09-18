import { useState } from 'react'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import {
  formatDate,
  isCancellable,
  statusBadgeVariant,
  titleCase,
} from '@/features/enrollments/enrollmentDisplay'
import { useCancelEnrollmentMutation, useMyEnrollmentsQuery } from '@/features/enrollments/hooks'

const PAGE_SIZE = 20

/**
 * The Student's own registrations, per
 * docs/tasks/TCM-16-frontend-course-catalog-enrollment.md step 3. Enrolling
 * happens on the catalog (CourseCatalogPage); this page tracks what came of
 * it and lets the student withdraw a request that's still live.
 */
export function MyEnrollmentsPage() {
  const [page, setPage] = useState(0)
  const [cancelTarget, setCancelTarget] = useState(null)

  const { data, isLoading } = useMyEnrollmentsQuery({ page, size: PAGE_SIZE, sort: 'enrolledAt,desc' })
  const cancelEnrollment = useCancelEnrollmentMutation()

  const enrollments = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  function confirmCancel() {
    cancelEnrollment.mutate(cancelTarget.id, { onSuccess: () => setCancelTarget(null) })
  }

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>My Enrollments</CardTitle>
        </CardHeader>

        <CardContent className="space-y-4">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Course</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Requested</TableHead>
                <TableHead>Decided</TableHead>
                <TableHead className="w-24" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading && (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground">
                    Loading…
                  </TableCell>
                </TableRow>
              )}

              {!isLoading && enrollments.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground">
                    You haven't enrolled in any course yet - browse the catalog to get started.
                  </TableCell>
                </TableRow>
              )}

              {enrollments.map((enrollment) => (
                <TableRow key={enrollment.id}>
                  <TableCell>
                    {enrollment.course.name}{' '}
                    <span className="text-xs text-muted-foreground">{enrollment.course.code}</span>
                  </TableCell>
                  <TableCell>
                    <Badge variant={statusBadgeVariant(enrollment.status)}>{titleCase(enrollment.status)}</Badge>
                  </TableCell>
                  <TableCell>{formatDate(enrollment.enrolledAt)}</TableCell>
                  <TableCell>{enrollment.decidedAt ? formatDate(enrollment.decidedAt) : '—'}</TableCell>
                  <TableCell>
                    {isCancellable(enrollment.status) && (
                      <Button variant="outline" size="sm" onClick={() => setCancelTarget(enrollment)}>
                        Cancel
                      </Button>
                    )}
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

      <AlertDialog open={Boolean(cancelTarget)} onOpenChange={(next) => !next && setCancelTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Cancel enrollment?</AlertDialogTitle>
            <AlertDialogDescription>
              Your registration for {cancelTarget?.course.name} will be withdrawn, and you won't be able to
              request this course again.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Keep it</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={cancelEnrollment.isPending}
              onClick={(event) => {
                event.preventDefault()
                confirmCancel()
              }}
            >
              Cancel enrollment
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
