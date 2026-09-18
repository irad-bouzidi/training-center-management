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
import { useDecideEnrollmentMutation } from './hooks'

const DECISIONS = {
  APPROVED: {
    label: 'Approve',
    title: 'Approve enrollment?',
    description: (enrollment) =>
      `${enrollment.student.name} takes a seat in ${enrollment.course.name}.`,
  },
  REJECTED: {
    label: 'Reject',
    title: 'Reject enrollment?',
    description: (enrollment) =>
      `${enrollment.student.name} won't be enrolled in ${enrollment.course.name}, and can't request it again.`,
  },
}

/**
 * Approve/Reject for one pending enrollment, each behind a confirm dialog -
 * see docs/tasks/TCM-16-frontend-course-catalog-enrollment.md step 4. Both
 * are the point of the approvals queue, so they sit inline on the row rather
 * than behind a kebab menu the way the course/user row actions do.
 *
 * Only PENDING enrollments are decidable (EnrollmentServiceImpl#decide), so a
 * decided row renders nothing.
 */
export function EnrollmentRowActions({ enrollment }) {
  const [pendingDecision, setPendingDecision] = useState(null)
  const decide = useDecideEnrollmentMutation()

  if (enrollment.status !== 'PENDING') {
    return null
  }

  const decision = pendingDecision && DECISIONS[pendingDecision]

  function confirmDecision() {
    decide.mutate({ id: enrollment.id, status: pendingDecision }, { onSuccess: () => setPendingDecision(null) })
  }

  return (
    <>
      <div className="flex justify-end gap-2">
        <Button size="sm" onClick={() => setPendingDecision('APPROVED')}>
          Approve
        </Button>
        <Button size="sm" variant="outline" onClick={() => setPendingDecision('REJECTED')}>
          Reject
        </Button>
      </div>

      <AlertDialog open={Boolean(pendingDecision)} onOpenChange={(next) => !next && setPendingDecision(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{decision?.title}</AlertDialogTitle>
            <AlertDialogDescription>{decision?.description(enrollment)}</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              variant={pendingDecision === 'REJECTED' ? 'destructive' : 'default'}
              disabled={decide.isPending}
              onClick={(event) => {
                event.preventDefault()
                confirmDecision()
              }}
            >
              {decision?.label}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
