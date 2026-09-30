import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { createUser, getUser, listUsers, resetPassword, setUserStatus, updateUser } from '@/api/userApi'
import { apiErrorMessage } from '@/api/serverErrors'

export const usersKeys = {
  all: ['users'],
  lists: () => [...usersKeys.all, 'list'],
  list: (params) => [...usersKeys.lists(), params],
  details: () => [...usersKeys.all, 'detail'],
  detail: (id) => [...usersKeys.details(), id],
}

export function useUsersQuery(params) {
  return useQuery({
    queryKey: usersKeys.list(params),
    queryFn: () => listUsers(params),
    // Keeps the current page's rows on screen while the next page loads,
    // instead of the table flashing empty between pages/filter changes.
    placeholderData: keepPreviousData,
  })
}

export function useUserQuery(id) {
  return useQuery({
    queryKey: usersKeys.detail(id),
    queryFn: () => getUser(id),
    enabled: Boolean(id),
  })
}

export function useCreateUserMutation() {
  const { t } = useTranslation('users')
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: usersKeys.lists() })
      toast.success(t('toasts.created'))
    },
    onError: (error) => toast.error(apiErrorMessage(error, t('toasts.createFailed'))),
  })
}

export function useUpdateUserMutation() {
  const { t } = useTranslation('users')
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, ...payload }) => updateUser(id, payload),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: usersKeys.lists() })
      queryClient.invalidateQueries({ queryKey: usersKeys.detail(variables.id) })
      toast.success(t('toasts.updated'))
    },
    onError: (error) => toast.error(apiErrorMessage(error, t('toasts.updateFailed'))),
  })
}

export function useSetUserStatusMutation() {
  const { t } = useTranslation('users')
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, status }) => setUserStatus(id, status),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: usersKeys.lists() })
      queryClient.invalidateQueries({ queryKey: usersKeys.detail(variables.id) })
      toast.success(data.status === 'ACTIVE' ? t('toasts.activated') : t('toasts.deactivated'))
    },
    onError: (error) => toast.error(apiErrorMessage(error, t('toasts.statusFailed'))),
  })
}

export function useResetPasswordMutation() {
  const { t } = useTranslation('users')

  return useMutation({
    // No success toast here - the caller shows the temp password in a
    // dialog, which is confirmation enough.
    mutationFn: resetPassword,
    onError: (error) => toast.error(apiErrorMessage(error, t('toasts.resetPasswordFailed'))),
  })
}
