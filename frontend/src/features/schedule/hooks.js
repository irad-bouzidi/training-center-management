import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { createSession, listSessions, setSessionStatus, updateSession } from '@/api/scheduleApi'

export const sessionsKeys = {
  all: ['sessions'],
  lists: () => [...sessionsKeys.all, 'list'],
  list: (params) => [...sessionsKeys.lists(), params],
}

// The backend never has a message body it can't produce (see
// GlobalExceptionHandler) - falling back to a generic string only covers a
// network-level failure (no response at all).
export function errorMessage(error, fallback) {
  return error.response?.data?.message ?? fallback
}

export function useSessionsQuery(params, options) {
  return useQuery({
    queryKey: sessionsKeys.list(params),
    queryFn: () => listSessions(params),
    // Keeps the current page's rows on screen while the next page loads,
    // instead of the agenda flashing empty between pages/filter changes.
    placeholderData: keepPreviousData,
    ...options,
  })
}

/**
 * Create/update deliberately have no error toast: both are submitted from
 * ScheduleFormDialog, which stays open on failure and shows the server's
 * message inline - a double-booking says which of the trainer or classroom
 * clashed, which belongs next to the fields it's about rather than in a
 * toast that outlives the dialog.
 */
export function useCreateSessionMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createSession,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sessionsKeys.all })
      toast.success('Session scheduled')
    },
  })
}

export function useUpdateSessionMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, ...payload }) => updateSession(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sessionsKeys.all })
      toast.success('Session updated')
    },
  })
}

export function useSetSessionStatusMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, status }) => setSessionStatus(id, status),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: sessionsKeys.all })
      toast.success(data.status === 'CANCELLED' ? 'Session cancelled' : 'Session marked completed')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to update session')),
  })
}
