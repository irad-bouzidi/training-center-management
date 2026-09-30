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
import { Button } from '@/components/ui/button'
import { CompleteEnrollmentButton } from './CompleteEnrollmentButton'
import { useDecideEnrollmentMutation } from './hooks'

// `enrollments:` i18n keys; the description interpolates {{student}} / {{course}}.
const DECISIONS = {
  APPROVED: {
    labelKey: 'decisions.approve',
    titleKey: 'decisions.approveTitle',
    descriptionKey: 'decisions.approveDescription',
  },
  REJECTED: {
    labelKey: 'decisions.reject',
    titleKey: 'decisions.rejectTitle',
    descriptionKey: 'decisions.rejectDescription',
  },
}

/**
 * Approve/Reject for one pending enrollment, each behind a confirm dialog -
 * see docs/tasks/TCM-16-frontend-course-catalog-enrollment.md step 4. Both
 * are the point of the approvals queue, so they sit inline on the row rather
 * than behind a kebab menu the way the course/user row actions do.
 *
 * Only PENDING enrollments are decidable (EnrollmentServiceImpl#decide). An
 * APPROVED one gets the next step instead - "Mark completed", which is what
 * lets a certificate be issued - and any other status renders nothing.
 */
export function EnrollmentRowActions({ enrollment }) {
  const { t } = useTranslation('enrollments')
  const [pendingDecision, setPendingDecision] = useState(null)
  const decide = useDecideEnrollmentMutation()

  if (enrollment.status === 'APPROVED') {
    return (
      <div className="flex justify-end">
        <CompleteEnrollmentButton enrollment={enrollment} />
      </div>
    )
  }

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
          {t('decisions.approve')}
        </Button>
        <Button size="sm" variant="outline" onClick={() => setPendingDecision('REJECTED')}>
          {t('decisions.reject')}
        </Button>
      </div>

      <AlertDialog open={Boolean(pendingDecision)} onOpenChange={(next) => !next && setPendingDecision(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{decision && t(decision.titleKey)}</AlertDialogTitle>
            <AlertDialogDescription>
              {decision &&
                t(decision.descriptionKey, { student: enrollment.student.name, course: enrollment.course.name })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('common:actions.cancel')}</AlertDialogCancel>
            <AlertDialogAction
              variant={pendingDecision === 'REJECTED' ? 'destructive' : 'default'}
              disabled={decide.isPending}
              onClick={(event) => {
                event.preventDefault()
                confirmDecision()
              }}
            >
              {decision && t(decision.labelKey)}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
