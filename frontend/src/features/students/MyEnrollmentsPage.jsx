import { useState } from 'react'
import { useTranslation } from 'react-i18next'
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
  const { t } = useTranslation('students')
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
          <CardTitle>{t('myEnrollments.title')}</CardTitle>
        </CardHeader>

        <CardContent className="space-y-4">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('myEnrollments.columns.course')}</TableHead>
                <TableHead>{t('myEnrollments.columns.status')}</TableHead>
                <TableHead>{t('myEnrollments.columns.requested')}</TableHead>
                <TableHead>{t('myEnrollments.columns.decided')}</TableHead>
                <TableHead className="w-24" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading && (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground">
                    {t('common:states.loading')}
                  </TableCell>
                </TableRow>
              )}

              {!isLoading && enrollments.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground">
                    {t('myEnrollments.empty')}
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
                    <Badge variant={statusBadgeVariant(enrollment.status)}>{t(`common:enums.enrollmentStatus.${enrollment.status}`)}</Badge>
                  </TableCell>
                  <TableCell>{formatDate(enrollment.enrolledAt)}</TableCell>
                  <TableCell>{enrollment.decidedAt ? formatDate(enrollment.decidedAt) : '—'}</TableCell>
                  <TableCell>
                    {isCancellable(enrollment.status) && (
                      <Button variant="outline" size="sm" onClick={() => setCancelTarget(enrollment)}>
                        {t('common:actions.cancel')}
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          <div className="flex items-center justify-between text-sm text-muted-foreground">
            <p>{t('myEnrollments.count', { count: totalElements })}</p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((current) => current - 1)}
              >
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
        </CardContent>
      </Card>

      <AlertDialog open={Boolean(cancelTarget)} onOpenChange={(next) => !next && setCancelTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t('myEnrollments.cancelConfirm.title')}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('myEnrollments.cancelConfirm.description', { course: cancelTarget?.course.name })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('myEnrollments.cancelConfirm.keep')}</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={cancelEnrollment.isPending}
              onClick={(event) => {
                event.preventDefault()
                confirmCancel()
              }}
            >
              {t('myEnrollments.cancelConfirm.confirm')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
