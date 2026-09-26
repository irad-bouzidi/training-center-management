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
import { Button } from '@/components/ui/button'
import { useCompleteEnrollmentMutation } from './hooks'

/**
 * The ADMIN's "Mark completed" for one APPROVED enrollment, behind a confirm
 * dialog. COMPLETED is the status a certificate requires, and there is no way
 * back from it, so it is never a single click. Rendered by every place an
 * admin manages enrollments - the approvals queue, a course's roster, and the
 * student summary's Enrollments and Certificates tabs.
 *
 * Only APPROVED enrollments can be completed (EnrollmentServiceImpl#
 * markCompleted), so any other status renders nothing; callers are
 * responsible for showing it to admins only.
 */
export function CompleteEnrollmentButton({ enrollment, size = 'sm', variant = 'outline' }) {
  const [confirming, setConfirming] = useState(false)
  const complete = useCompleteEnrollmentMutation()

  if (enrollment.status !== 'APPROVED') {
    return null
  }

  return (
    <>
      <Button size={size} variant={variant} onClick={() => setConfirming(true)}>
        Mark completed
      </Button>

      <AlertDialog open={confirming} onOpenChange={setConfirming}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Mark enrollment completed?</AlertDialogTitle>
            <AlertDialogDescription>
              {enrollment.student?.name ?? 'This student'} has finished {enrollment.course.name}. Once completed, the
              enrollment can’t be reopened, and a certificate can be issued if their attendance qualifies.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              disabled={complete.isPending}
              onClick={(event) => {
                event.preventDefault()
                complete.mutate(enrollment.id, { onSuccess: () => setConfirming(false) })
              }}
            >
              Mark completed
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
