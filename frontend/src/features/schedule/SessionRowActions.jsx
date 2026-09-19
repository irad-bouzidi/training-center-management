import { CheckCircle, ClipboardCheck, MoreHorizontal, Pencil, X } from 'lucide-react'
import { useState } from 'react'
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
          {canTakeAttendance && (
            <DropdownMenuItem
              onSelect={() => navigate(`/${user.role.toLowerCase()}/sessions/${session.id}/attendance`)}
            >
              <ClipboardCheck />
              Take attendance
            </DropdownMenuItem>
          )}
          {isAdmin && session.status === 'SCHEDULED' && (
            <DropdownMenuItem onSelect={() => onEdit(session)}>
              <Pencil />
              Edit
            </DropdownMenuItem>
          )}
          {canComplete && session.status === 'SCHEDULED' && (
            <DropdownMenuItem onSelect={() => setCompleteConfirmOpen(true)}>
              <CheckCircle />
              Mark completed
            </DropdownMenuItem>
          )}
          {isAdmin && session.status === 'SCHEDULED' && (
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
