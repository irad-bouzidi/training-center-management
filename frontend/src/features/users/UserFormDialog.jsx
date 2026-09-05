import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
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
import { ROLE_OPTIONS, titleCase } from './userDisplay'

// Mirrors backend/src/main/java/com/tcm/user/dto/UserRequest.java. Same
// stub-shadcn-Form situation as LoginPage (see TCM-9) - react-hook-form is
// composed directly against Label/Input/Select instead.
const editSchema = z.object({
  firstName: z.string().min(1, 'First name is required'),
  lastName: z.string().min(1, 'Last name is required'),
  email: z.string().min(1, 'Email is required').email('Enter a valid email'),
  phone: z.string().optional(),
  role: z.enum(ROLE_OPTIONS, 'Role is required'),
})
const createSchema = editSchema.extend({
  password: z.string().min(1, 'Password is required'),
})

const EMPTY_VALUES = { firstName: '', lastName: '', email: '', phone: '', role: undefined, password: '' }

/**
 * Create/edit dialog. `user` is null for create; an existing user for edit
 * (password field hidden, status toggle shown - see
 * docs/tasks/TCM-10-frontend-user-management.md step 3).
 */
export function UserFormDialog({ open, onOpenChange, user }) {
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
          <DialogTitle>{isEdit ? 'Edit user' : 'New user'}</DialogTitle>
          <DialogDescription>
            {isEdit ? `Update ${user.firstName} ${user.lastName}'s account.` : 'Create a new Admin, Trainer, or Student account.'}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="firstName">First name</Label>
              <Input id="firstName" aria-invalid={Boolean(errors.firstName)} {...register('firstName')} />
              {errors.firstName && <p className="text-sm text-destructive">{errors.firstName.message}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="lastName">Last name</Label>
              <Input id="lastName" aria-invalid={Boolean(errors.lastName)} {...register('lastName')} />
              {errors.lastName && <p className="text-sm text-destructive">{errors.lastName.message}</p>}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" aria-invalid={Boolean(errors.email)} {...register('email')} />
            {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
          </div>

          {!isEdit && (
            <div className="space-y-2">
              <Label htmlFor="password">Temporary password</Label>
              <Input id="password" type="password" aria-invalid={Boolean(errors.password)} {...register('password')} />
              {errors.password && <p className="text-sm text-destructive">{errors.password.message}</p>}
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="phone">Phone</Label>
            <Input id="phone" type="tel" {...register('phone')} />
          </div>

          <div className="space-y-2">
            <Label htmlFor="role">Role</Label>
            <Controller
              name="role"
              control={control}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger id="role" className="w-full" aria-invalid={Boolean(errors.role)}>
                    <SelectValue placeholder="Select a role" />
                  </SelectTrigger>
                  <SelectContent>
                    {ROLE_OPTIONS.map((role) => (
                      <SelectItem key={role} value={role}>
                        {titleCase(role)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.role && <p className="text-sm text-destructive">{errors.role.message}</p>}
          </div>

          {isEdit && (
            <div className="flex items-center justify-between rounded-lg border p-3">
              <div>
                <Label htmlFor="status-toggle">Active</Label>
                <p className="text-sm text-muted-foreground">Inactive users can't sign in.</p>
              </div>
              <Switch id="status-toggle" checked={statusActive} onCheckedChange={setStatusActive} />
            </div>
          )}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Saving…' : 'Save'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
