import { CheckCircle, ClipboardCheck, MoreHorizontal, Pencil, X } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
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
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useAuth } from '@/context/AuthContext'
import { formatSessionDate, formatTimeRange } from './scheduleDisplay'
import { useSetSessionStatusMutation } from './hooks'

/**
 * Per-session actions, following the CourseRowActions/UserRowActions pattern:
 * a kebab menu plus the confirm dialogs its entries trigger.
 *
 * What's on the menu follows what the backend allows: an ADMIN can
 * reschedule, complete or cancel; the assigned TRAINER can only mark their
 * own session completed (ClassSessionController#changeStatus). Either may
 * take the session's attendance (TCM-19), including after it has run, which
 * is the one entry that outlives the SCHEDULED state. A cancelled session
 * has nothing left to do, so its menu is dropped rather than rendered empty.
 */
export function SessionRowActions({ session, onEdit }) {
  const { t } = useTranslation('schedule')
  const { user } = useAuth()
  const navigate = useNavigate()
  const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false)
  const [completeConfirmOpen, setCompleteConfirmOpen] = useState(false)
  const setSessionStatus = useSetSessionStatusMutation()

  const isAdmin = user.role === 'ADMIN'
  // An admin may complete any session; anyone else only one they're assigned
  // to. The same pair may take its attendance (TCM-19), and unlike the status
  // actions that stays useful after the session has run - a roster is often
  // marked once the class is over.
  const canComplete = isAdmin || session.trainer.id === user.id
  const canTakeAttendance = canComplete && session.status !== 'CANCELLED'

  if (!canTakeAttendance) {
    return null
  }

  function changeStatus(status, closeDialog) {
    setSessionStatus.mutate({ id: session.id, status }, { onSuccess: () => closeDialog(false) })
  }

  const when = t('actions.when', {
    date: formatSessionDate(session.sessionDate),
    time: formatTimeRange(session.startTime, session.endTime),
  })
  const course = session.course.name

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon-sm" aria-label={t('actions.menuLabel', { course, when })}>
            <MoreHorizontal />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {canTakeAttendance && (
            <DropdownMenuItem
              onSelect={() => navigate(`/${user.role.toLowerCase()}/sessions/${session.id}/attendance`)}
            >
              <ClipboardCheck />
              {t('actions.takeAttendance')}
            </DropdownMenuItem>
          )}
          {isAdmin && session.status === 'SCHEDULED' && (
            <DropdownMenuItem onSelect={() => onEdit(session)}>
              <Pencil />
              {t('common:actions.edit')}
            </DropdownMenuItem>
          )}
          {canComplete && session.status === 'SCHEDULED' && (
            <DropdownMenuItem onSelect={() => setCompleteConfirmOpen(true)}>
              <CheckCircle />
              {t('actions.markCompleted')}
            </DropdownMenuItem>
          )}
          {isAdmin && session.status === 'SCHEDULED' && (
            <>
              <DropdownMenuSeparator />
              <DropdownMenuItem variant="destructive" onSelect={() => setCancelConfirmOpen(true)}>
                <X />
                {t('actions.cancelSession')}
              </DropdownMenuItem>
            </>
          )}
        </DropdownMenuContent>
      </DropdownMenu>

      <AlertDialog open={completeConfirmOpen} onOpenChange={setCompleteConfirmOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t('actions.completeConfirm.title')}</AlertDialogTitle>
            <AlertDialogDescription>{t('actions.completeConfirm.description', { course, when })}</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('common:actions.cancel')}</AlertDialogCancel>
            <AlertDialogAction
              disabled={setSessionStatus.isPending}
              onClick={(event) => {
                event.preventDefault()
                changeStatus('COMPLETED', setCompleteConfirmOpen)
              }}
            >
              {t('actions.markCompleted')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={cancelConfirmOpen} onOpenChange={setCancelConfirmOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t('actions.cancelConfirm.title')}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('actions.cancelConfirm.description', { course, when, classroom: session.classroom })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('actions.cancelConfirm.keep')}</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={setSessionStatus.isPending}
              onClick={(event) => {
                event.preventDefault()
                changeStatus('CANCELLED', setCancelConfirmOpen)
              }}
            >
              {t('actions.cancelSession')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
