import { Check, KeyRound, MoreHorizontal, Pencil, X } from 'lucide-react'
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
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import { useResetPasswordMutation, useSetUserStatusMutation } from './hooks'
import { fullName } from './userDisplay'

/**
 * Dropdown menu of per-row actions (view/edit/activate-deactivate/reset
 * password), plus the confirm dialogs and temp-password reveal those trigger
 * - see docs/tasks/TCM-10-frontend-user-management.md step 4.
 */
export function UserRowActions({ user, onView, onEdit }) {
  const [statusConfirmOpen, setStatusConfirmOpen] = useState(false)
  const [resetConfirmOpen, setResetConfirmOpen] = useState(false)
  const [tempPassword, setTempPassword] = useState(null)

  const setUserStatus = useSetUserStatusMutation()
  const resetPassword = useResetPasswordMutation()

  const isActive = user.status === 'ACTIVE'
  const nextStatus = isActive ? 'INACTIVE' : 'ACTIVE'

  function confirmStatusChange() {
    setUserStatus.mutate(
      { id: user.id, status: nextStatus },
      { onSuccess: () => setStatusConfirmOpen(false) },
    )
  }

  function confirmResetPassword() {
    resetPassword.mutate(user.id, {
      onSuccess: (data) => {
        setResetConfirmOpen(false)
        setTempPassword(data.tempPassword)
      },
    })
  }

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon-sm" aria-label={`Actions for ${fullName(user)}`}>
            <MoreHorizontal />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onSelect={() => onView(user)}>View details</DropdownMenuItem>
          <DropdownMenuItem onSelect={() => onEdit(user)}>
            <Pencil />
            Edit
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem onSelect={() => setStatusConfirmOpen(true)} variant={isActive ? 'destructive' : 'default'}>
            {isActive ? <X /> : <Check />}
            {isActive ? 'Deactivate' : 'Activate'}
          </DropdownMenuItem>
          <DropdownMenuItem onSelect={() => setResetConfirmOpen(true)}>
            <KeyRound />
            Reset password
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <AlertDialog open={statusConfirmOpen} onOpenChange={setStatusConfirmOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{isActive ? 'Deactivate user?' : 'Activate user?'}</AlertDialogTitle>
            <AlertDialogDescription>
              {isActive
                ? `${fullName(user)} will no longer be able to sign in.`
                : `${fullName(user)} will be able to sign in again.`}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              variant={isActive ? 'destructive' : 'default'}
              disabled={setUserStatus.isPending}
              onClick={(event) => {
                event.preventDefault()
                confirmStatusChange()
              }}
            >
              {isActive ? 'Deactivate' : 'Activate'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={resetConfirmOpen} onOpenChange={setResetConfirmOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Reset password?</AlertDialogTitle>
            <AlertDialogDescription>
              This generates a new temporary password for {fullName(user)} and invalidates their current one.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              disabled={resetPassword.isPending}
              onClick={(event) => {
                event.preventDefault()
                confirmResetPassword()
              }}
            >
              Reset password
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <Dialog open={Boolean(tempPassword)} onOpenChange={(next) => !next && setTempPassword(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Temporary password</DialogTitle>
            <DialogDescription>
              Share this with {fullName(user)} out of band - it won't be shown again.
            </DialogDescription>
          </DialogHeader>
          <Input readOnly value={tempPassword ?? ''} className="font-mono" onFocus={(e) => e.target.select()} />
          <DialogFooter>
            <Button onClick={() => setTempPassword(null)}>Done</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}
