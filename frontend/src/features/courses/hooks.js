import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { createCourse, getCourse, listCourses, listMyCourses, setCourseStatus, updateCourse } from '@/api/courseApi'
import { listUsers } from '@/api/userApi'
import { apiErrorMessage } from '@/api/serverErrors'

export const coursesKeys = {
  all: ['courses'],
  lists: () => [...coursesKeys.all, 'list'],
  list: (params) => [...coursesKeys.lists(), params],
  details: () => [...coursesKeys.all, 'detail'],
  detail: (id) => [...coursesKeys.details(), id],
  mine: (params) => [...coursesKeys.all, 'mine', params],
}

export function useCoursesQuery(params, options) {
  return useQuery({
    queryKey: coursesKeys.list(params),
    queryFn: () => listCourses(params),
    // Keeps the current page's rows on screen while the next page loads,
    // instead of the table/grid flashing empty between pages/filter changes.
    placeholderData: keepPreviousData,
    ...options,
  })
}

/** The TRAINER's own courses, all statuses - `options` lets a caller gate it
 * on role, since /courses/mine 403s for anyone else. */
export function useMyCoursesQuery(params, options) {
  return useQuery({
    queryKey: coursesKeys.mine(params),
    queryFn: () => listMyCourses(params),
    placeholderData: keepPreviousData,
    ...options,
  })
}

export function useCourseQuery(id) {
  return useQuery({
    queryKey: coursesKeys.detail(id),
    queryFn: () => getCourse(id),
    enabled: Boolean(id),
  })
}

// Trainer options for the course and schedule forms' Selects - one large page
// is enough for the trainer roster this app expects, so no pagination here.
// `options` lets a caller gate the fetch on role (the schedule agenda only
// needs the roster for its admin-only filter).
export function useTrainersQuery(options) {
  return useQuery({
    queryKey: ['users', 'list', { role: 'TRAINER', size: 200, forPicker: true }],
    queryFn: () => listUsers({ role: 'TRAINER', size: 200 }),
    select: (data) => data.content,
    ...options,
  })
}

export function useCreateCourseMutation() {
  const { t } = useTranslation('courses')
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createCourse,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: coursesKeys.lists() })
      toast.success(t('toasts.created'))
    },
    onError: (error) => toast.error(apiErrorMessage(error, t('toasts.createFailed'))),
  })
}

export function useUpdateCourseMutation() {
  const { t } = useTranslation('courses')
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, ...payload }) => updateCourse(id, payload),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: coursesKeys.lists() })
      queryClient.invalidateQueries({ queryKey: coursesKeys.detail(variables.id) })
      toast.success(t('toasts.updated'))
    },
    onError: (error) => toast.error(apiErrorMessage(error, t('toasts.updateFailed'))),
  })
}

export function useSetCourseStatusMutation() {
  const { t } = useTranslation('courses')
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, status }) => setCourseStatus(id, status),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: coursesKeys.lists() })
      queryClient.invalidateQueries({ queryKey: coursesKeys.detail(variables.id) })
      toast.success(
        data.status === 'PUBLISHED'
          ? t('toasts.published')
          : data.status === 'ARCHIVED'
            ? t('toasts.archived')
            : t('toasts.movedToDraft'),
      )
    },
    onError: (error) => toast.error(apiErrorMessage(error, t('toasts.statusFailed'))),
  })
}
