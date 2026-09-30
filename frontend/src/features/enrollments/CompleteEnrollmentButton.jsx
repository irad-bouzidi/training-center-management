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
  const { t } = useTranslation('enrollments')
  const [confirming, setConfirming] = useState(false)
  const complete = useCompleteEnrollmentMutation()

  if (enrollment.status !== 'APPROVED') {
    return null
  }

  return (
    <>
      <Button size={size} variant={variant} onClick={() => setConfirming(true)}>
        {t('complete.button')}
      </Button>

      <AlertDialog open={confirming} onOpenChange={setConfirming}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t('complete.title')}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('complete.description', {
                student: enrollment.student?.name ?? t('complete.thisStudent'),
                course: enrollment.course.name,
              })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('common:actions.cancel')}</AlertDialogCancel>
            <AlertDialogAction
              disabled={complete.isPending}
              onClick={(event) => {
                event.preventDefault()
                complete.mutate(enrollment.id, { onSuccess: () => setConfirming(false) })
              }}
            >
              {t('complete.button')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
