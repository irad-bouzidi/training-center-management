import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { createUser, getUser, listUsers, resetPassword, setUserStatus, updateUser } from '@/api/userApi'

export const usersKeys = {
  all: ['users'],
  lists: () => [...usersKeys.all, 'list'],
  list: (params) => [...usersKeys.lists(), params],
  details: () => [...usersKeys.all, 'detail'],
  detail: (id) => [...usersKeys.details(), id],
}

// The backend never has a message body it can't produce (see
// GlobalExceptionHandler) - falling back to a generic string only covers a
// network-level failure (no response at all).
function errorMessage(error, fallback) {
  return error.response?.data?.message ?? fallback
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
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: usersKeys.lists() })
      toast.success('User created')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to create user')),
  })
}

export function useUpdateUserMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, ...payload }) => updateUser(id, payload),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: usersKeys.lists() })
      queryClient.invalidateQueries({ queryKey: usersKeys.detail(variables.id) })
      toast.success('User updated')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to update user')),
  })
}

export function useSetUserStatusMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, status }) => setUserStatus(id, status),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: usersKeys.lists() })
      queryClient.invalidateQueries({ queryKey: usersKeys.detail(variables.id) })
      toast.success(data.status === 'ACTIVE' ? 'User activated' : 'User deactivated')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to update status')),
  })
}

export function useResetPasswordMutation() {
  return useMutation({
    // No success toast here - the caller shows the temp password in a
    // dialog, which is confirmation enough.
    mutationFn: resetPassword,
    onError: (error) => toast.error(errorMessage(error, 'Failed to reset password')),
  })
}
