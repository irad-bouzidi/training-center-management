import { CheckCircle, MoreHorizontal, Pencil, X } from 'lucide-react'
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
 * What's on the menu follows what the backend allows (see
 * ClassSessionController#changeStatus): an ADMIN can reschedule, complete or
 * cancel; the assigned TRAINER can only mark their own session completed. A
 * session that has already been cancelled or completed is finished with - the menu is
 * dropped entirely rather than rendered empty.
 */
export function SessionRowActions({ session, onEdit }) {
  const { user } = useAuth()
  const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false)
  const [completeConfirmOpen, setCompleteConfirmOpen] = useState(false)
  const setSessionStatus = useSetSessionStatusMutation()

  const isAdmin = user.role === 'ADMIN'
  // An admin may complete any session; anyone else only one they're assigned
  // to. Nobody else has an action here at all, so the menu goes away.
  const canComplete = isAdmin || session.trainer.id === user.id

  if (session.status !== 'SCHEDULED' || !canComplete) {
    return null
  }

  function changeStatus(status, closeDialog) {
    setSessionStatus.mutate({ id: session.id, status }, { onSuccess: () => closeDialog(false) })
  }

  const when = `${formatSessionDate(session.sessionDate)}, ${formatTimeRange(session.startTime, session.endTime)}`

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon-sm" aria-label={`Actions for ${session.course.name} on ${when}`}>
            <MoreHorizontal />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {isAdmin && (
            <DropdownMenuItem onSelect={() => onEdit(session)}>
              <Pencil />
              Edit
            </DropdownMenuItem>
          )}
          {canComplete && (
            <DropdownMenuItem onSelect={() => setCompleteConfirmOpen(true)}>
              <CheckCircle />
              Mark completed
            </DropdownMenuItem>
          )}
          {isAdmin && (
            <>
              <DropdownMenuSeparator />
              <DropdownMenuItem variant="destructive" onSelect={() => setCancelConfirmOpen(true)}>
                <X />
                Cancel session
              </DropdownMenuItem>
            </>
          )}
        </DropdownMenuContent>
      </DropdownMenu>

      <AlertDialog open={completeConfirmOpen} onOpenChange={setCompleteConfirmOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Mark session completed?</AlertDialogTitle>
            <AlertDialogDescription>
              {session.course.name} on {when} will be recorded as delivered. This can't be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              disabled={setSessionStatus.isPending}
              onClick={(event) => {
                event.preventDefault()
                changeStatus('COMPLETED', setCompleteConfirmOpen)
              }}
            >
              Mark completed
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={cancelConfirmOpen} onOpenChange={setCancelConfirmOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Cancel session?</AlertDialogTitle>
            <AlertDialogDescription>
              {session.course.name} on {when} will be called off, freeing {session.classroom} and its trainer for
              other bookings. A cancelled session can't be reopened.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Keep it</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={setSessionStatus.isPending}
              onClick={(event) => {
                event.preventDefault()
                changeStatus('CANCELLED', setCancelConfirmOpen)
              }}
            >
              Cancel session
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
