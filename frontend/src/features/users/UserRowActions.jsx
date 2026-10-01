import { Check, KeyRound, MoreHorizontal, Pencil, X } from 'lucide-react'
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
  const { t } = useTranslation('users')
  const [statusConfirmOpen, setStatusConfirmOpen] = useState(false)
  const [resetConfirmOpen, setResetConfirmOpen] = useState(false)
  const [tempPassword, setTempPassword] = useState(null)

  const setUserStatus = useSetUserStatusMutation()
  const resetPassword = useResetPasswordMutation()

  const isActive = user.status === 'ACTIVE'
  const nextStatus = isActive ? 'INACTIVE' : 'ACTIVE'
  const name = fullName(user)

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
          <Button variant="ghost" size="icon-sm" aria-label={t('actions.menuLabel', { name })}>
            <MoreHorizontal />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onSelect={() => onView(user)}>{t('actions.view')}</DropdownMenuItem>
          <DropdownMenuItem onSelect={() => onEdit(user)}>
            <Pencil />
            {t('common:actions.edit')}
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem onSelect={() => setStatusConfirmOpen(true)} variant={isActive ? 'destructive' : 'default'}>
            {isActive ? <X /> : <Check />}
            {isActive ? t('actions.deactivate') : t('actions.activate')}
          </DropdownMenuItem>
          <DropdownMenuItem onSelect={() => setResetConfirmOpen(true)}>
            <KeyRound />
            {t('actions.resetPassword')}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <AlertDialog open={statusConfirmOpen} onOpenChange={setStatusConfirmOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{isActive ? t('statusConfirm.deactivateTitle') : t('statusConfirm.activateTitle')}</AlertDialogTitle>
            <AlertDialogDescription>
              {isActive
                ? t('statusConfirm.deactivateDescription', { name })
                : t('statusConfirm.activateDescription', { name })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('common:actions.cancel')}</AlertDialogCancel>
            <AlertDialogAction
              variant={isActive ? 'destructive' : 'default'}
              disabled={setUserStatus.isPending}
              onClick={(event) => {
                event.preventDefault()
                confirmStatusChange()
              }}
            >
              {isActive ? t('actions.deactivate') : t('actions.activate')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={resetConfirmOpen} onOpenChange={setResetConfirmOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t('resetConfirm.title')}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('resetConfirm.description', { name })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('common:actions.cancel')}</AlertDialogCancel>
            <AlertDialogAction
              disabled={resetPassword.isPending}
              onClick={(event) => {
                event.preventDefault()
                confirmResetPassword()
              }}
            >
              {t('actions.resetPassword')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <Dialog open={Boolean(tempPassword)} onOpenChange={(next) => !next && setTempPassword(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('tempPassword.title')}</DialogTitle>
            <DialogDescription>
              {t('tempPassword.description', { name })}
            </DialogDescription>
          </DialogHeader>
          <Input readOnly value={tempPassword ?? ''} className="font-mono" onFocus={(e) => e.target.select()} />
          <DialogFooter>
            <Button onClick={() => setTempPassword(null)}>{t('tempPassword.done')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}
