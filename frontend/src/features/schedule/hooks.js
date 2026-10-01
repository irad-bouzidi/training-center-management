import { keepPreviousData, useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { createSession, listSessions, setSessionStatus, updateSession } from '@/api/scheduleApi'
import { apiErrorMessage } from '@/api/serverErrors'

export const sessionsKeys = {
  all: ['sessions'],
  lists: () => [...sessionsKeys.all, 'list'],
  list: (params) => [...sessionsKeys.lists(), params],
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
 * The same listing for several courses at once, one query per course - what
 * the student summary's Schedule tab needs, since there is no per-student
 * session filter for an admin or trainer to use. Each query keeps the
 * backend's role scoping, so a trainer only ever gets their own sessions.
 */
export function useSessionsForCoursesQueries(courseIds, params) {
  return useQueries({
    queries: courseIds.map((courseId) => {
      const queryParams = { ...params, courseId }
      return {
        queryKey: sessionsKeys.list(queryParams),
        queryFn: () => listSessions(queryParams),
      }
    }),
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
  const { t } = useTranslation('schedule')
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createSession,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sessionsKeys.all })
      toast.success(t('toasts.created'))
    },
  })
}

export function useUpdateSessionMutation() {
  const { t } = useTranslation('schedule')
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, ...payload }) => updateSession(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sessionsKeys.all })
      toast.success(t('toasts.updated'))
    },
  })
}

export function useSetSessionStatusMutation() {
  const { t } = useTranslation('schedule')
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, status }) => setSessionStatus(id, status),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: sessionsKeys.all })
      toast.success(data.status === 'CANCELLED' ? t('toasts.cancelled') : t('toasts.completed'))
    },
    onError: (error) => toast.error(apiErrorMessage(error, t('toasts.statusFailed'))),
  })
}
