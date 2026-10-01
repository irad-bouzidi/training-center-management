import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { useCreateUserMutation, useSetUserStatusMutation, useUpdateUserMutation } from './hooks'
import { ROLE_OPTIONS } from './userDisplay'

// Mirrors backend/src/main/java/com/tcm/user/dto/UserRequest.java. Same
// stub-shadcn-Form situation as LoginPage (see TCM-9) - react-hook-form is
// composed directly against Label/Input/Select instead. Messages are i18n
// keys (users namespace), translated where they render.
const editSchema = z.object({
  firstName: z.string().min(1, 'form.errors.firstNameRequired'),
  lastName: z.string().min(1, 'form.errors.lastNameRequired'),
  email: z.string().min(1, 'form.errors.emailRequired').email('form.errors.emailInvalid'),
  phone: z.string().optional(),
  role: z.enum(ROLE_OPTIONS, 'form.errors.roleRequired'),
})
const createSchema = editSchema.extend({
  password: z.string().min(1, 'form.errors.passwordRequired'),
})

const EMPTY_VALUES = { firstName: '', lastName: '', email: '', phone: '', role: undefined, password: '' }

/**
 * Create/edit dialog. `user` is null for create; an existing user for edit
 * (password field hidden, status toggle shown - see
 * docs/tasks/TCM-10-frontend-user-management.md step 3).
 */
export function UserFormDialog({ open, onOpenChange, user }) {
  const { t } = useTranslation('users')
  const isEdit = Boolean(user)
  // The parent remounts this dialog (via a `key` keyed on the user) each
  // time it's opened for a different user or for create, so a plain
  // initializer is enough - no effect needed to keep it in sync.
  const [statusActive, setStatusActive] = useState(user ? user.status === 'ACTIVE' : true)
  const createUser = useCreateUserMutation()
  const updateUser = useUpdateUserMutation()
  const setUserStatus = useSetUserStatusMutation()

  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(isEdit ? editSchema : createSchema),
    values: isEdit
      ? { firstName: user.firstName, lastName: user.lastName, email: user.email, phone: user.phone ?? '', role: user.role }
      : EMPTY_VALUES,
  })

  async function onSubmit(values) {
    try {
      if (isEdit) {
        await updateUser.mutateAsync({ id: user.id, ...values })
        const wasActive = user.status === 'ACTIVE'
        if (statusActive !== wasActive) {
          await setUserStatus.mutateAsync({ id: user.id, status: statusActive ? 'ACTIVE' : 'INACTIVE' })
        }
      } else {
        await createUser.mutateAsync(values)
      }
      onOpenChange(false)
    } catch {
      // Already surfaced via the mutation's onError toast (see hooks.js) -
      // keep the dialog open so the user can fix the input and retry.
    }
  }

  function handleOpenChange(next) {
    if (!next) reset(EMPTY_VALUES)
    onOpenChange(next)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? t('form.editTitle') : t('form.createTitle')}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? t('form.editDescription', { name: `${user.firstName} ${user.lastName}` })
              : t('form.createDescription')}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="firstName">{t('fields.firstName')}</Label>
              <Input id="firstName" aria-invalid={Boolean(errors.firstName)} {...register('firstName')} />
              {errors.firstName && <p className="text-sm text-destructive">{t(errors.firstName.message)}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="lastName">{t('fields.lastName')}</Label>
              <Input id="lastName" aria-invalid={Boolean(errors.lastName)} {...register('lastName')} />
              {errors.lastName && <p className="text-sm text-destructive">{t(errors.lastName.message)}</p>}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="email">{t('fields.email')}</Label>
            <Input id="email" type="email" aria-invalid={Boolean(errors.email)} {...register('email')} />
            {errors.email && <p className="text-sm text-destructive">{t(errors.email.message)}</p>}
          </div>

          {!isEdit && (
            <div className="space-y-2">
              <Label htmlFor="password">{t('form.temporaryPassword')}</Label>
              <Input id="password" type="password" aria-invalid={Boolean(errors.password)} {...register('password')} />
              {errors.password && <p className="text-sm text-destructive">{t(errors.password.message)}</p>}
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="phone">{t('fields.phone')}</Label>
            <Input id="phone" type="tel" {...register('phone')} />
          </div>

          <div className="space-y-2">
            <Label htmlFor="role">{t('fields.role')}</Label>
            <Controller
              name="role"
              control={control}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger id="role" className="w-full" aria-invalid={Boolean(errors.role)}>
                    <SelectValue placeholder={t('form.rolePlaceholder')} />
                  </SelectTrigger>
                  <SelectContent>
                    {ROLE_OPTIONS.map((role) => (
                      <SelectItem key={role} value={role}>
                        {t(`common:enums.role.${role}`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.role && <p className="text-sm text-destructive">{t(errors.role.message)}</p>}
          </div>

          {isEdit && (
            <div className="flex items-center justify-between rounded-lg border p-3">
              <div>
                <Label htmlFor="status-toggle">{t('common:enums.accountStatus.ACTIVE')}</Label>
                <p className="text-sm text-muted-foreground">{t('form.statusHint')}</p>
              </div>
              <Switch id="status-toggle" checked={statusActive} onCheckedChange={setStatusActive} />
            </div>
          )}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
              {t('common:actions.cancel')}
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? t('common:actions.saving') : t('common:actions.save')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
