import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { createCourse, getCourse, listCourses, setCourseStatus, updateCourse } from '@/api/courseApi'
import { listUsers } from '@/api/userApi'

export const coursesKeys = {
  all: ['courses'],
  lists: () => [...coursesKeys.all, 'list'],
  list: (params) => [...coursesKeys.lists(), params],
  details: () => [...coursesKeys.all, 'detail'],
  detail: (id) => [...coursesKeys.details(), id],
}

// The backend never has a message body it can't produce (see
// GlobalExceptionHandler) - falling back to a generic string only covers a
// network-level failure (no response at all).
function errorMessage(error, fallback) {
  return error.response?.data?.message ?? fallback
}

export function useCoursesQuery(params) {
  return useQuery({
    queryKey: coursesKeys.list(params),
    queryFn: () => listCourses(params),
    // Keeps the current page's rows on screen while the next page loads,
    // instead of the table/grid flashing empty between pages/filter changes.
    placeholderData: keepPreviousData,
  })
}

export function useCourseQuery(id) {
  return useQuery({
    queryKey: coursesKeys.detail(id),
    queryFn: () => getCourse(id),
    enabled: Boolean(id),
  })
}

// Trainer options for CourseFormDialog's Select - one large page is enough
// for the trainer roster this app expects, so no pagination here.
export function useTrainersQuery() {
  return useQuery({
    queryKey: ['users', 'list', { role: 'TRAINER', size: 200, forPicker: true }],
    queryFn: () => listUsers({ role: 'TRAINER', size: 200 }),
    select: (data) => data.content,
  })
}

export function useCreateCourseMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createCourse,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: coursesKeys.lists() })
      toast.success('Course created')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to create course')),
  })
}

export function useUpdateCourseMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, ...payload }) => updateCourse(id, payload),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: coursesKeys.lists() })
      queryClient.invalidateQueries({ queryKey: coursesKeys.detail(variables.id) })
      toast.success('Course updated')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to update course')),
  })
}

export function useSetCourseStatusMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, status }) => setCourseStatus(id, status),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: coursesKeys.lists() })
      queryClient.invalidateQueries({ queryKey: coursesKeys.detail(variables.id) })
      toast.success(
        data.status === 'PUBLISHED' ? 'Course published' : data.status === 'ARCHIVED' ? 'Course archived' : 'Course moved to draft',
      )
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to update status')),
  })
}
