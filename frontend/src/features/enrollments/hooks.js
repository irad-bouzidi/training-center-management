import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import {
  cancelEnrollment,
  decideEnrollment,
  listEnrollments,
  listMyEnrollments,
  registerEnrollment,
} from '@/api/enrollmentApi'
import { coursesKeys } from '@/features/courses/hooks'
import { studentsKeys } from '@/features/students/hooks'

export const enrollmentsKeys = {
  all: ['enrollments'],
  lists: () => [...enrollmentsKeys.all, 'list'],
  list: (params) => [...enrollmentsKeys.lists(), params],
  mine: (params) => [...enrollmentsKeys.all, 'mine', params],
}

// The backend never has a message body it can't produce (see
// GlobalExceptionHandler) - falling back to a generic string only covers a
// network-level failure (no response at all).
function errorMessage(error, fallback) {
  return error.response?.data?.message ?? fallback
}

export function useEnrollmentsQuery(params, options) {
  return useQuery({
    queryKey: enrollmentsKeys.list(params),
    queryFn: () => listEnrollments(params),
    // Keeps the current page's rows on screen while the next page loads,
    // instead of the table flashing empty between pages/filter changes.
    placeholderData: keepPreviousData,
    ...options,
  })
}

/** The STUDENT's own enrollments. Also used by the catalog to decide each
 * card's enroll state, hence `options` for the role guard there. */
export function useMyEnrollmentsQuery(params, options) {
  return useQuery({
    queryKey: enrollmentsKeys.mine(params),
    queryFn: () => listMyEnrollments(params),
    placeholderData: keepPreviousData,
    ...options,
  })
}

/**
 * The student's own enrollments keyed by course id - what the catalog needs
 * to label each card's enroll button. One page covers the enrollment count a
 * single student realistically has; `enabled` gates it on the STUDENT role,
 * since /enrollments/mine 403s for anyone else.
 */
export function useMyEnrollmentsByCourseQuery(enabled) {
  return useMyEnrollmentsQuery(
    { size: 200 },
    {
      enabled,
      select: (data) => new Map(data.content.map((enrollment) => [enrollment.course.id, enrollment])),
    },
  )
}

/**
 * An enrollment changing status moves a seat in or out of the course's
 * APPROVED count, which the catalog reads as `approvedCount`, and shows up in
 * the student directory's counts and summaries - so all three go stale
 * together.
 */
function invalidateAffected(queryClient) {
  queryClient.invalidateQueries({ queryKey: enrollmentsKeys.all })
  queryClient.invalidateQueries({ queryKey: coursesKeys.all })
  queryClient.invalidateQueries({ queryKey: studentsKeys.all })
}

export function useRegisterMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: registerEnrollment,
    onSuccess: () => {
      invalidateAffected(queryClient)
      toast.success('Enrollment requested')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to request enrollment')),
  })
}

export function useDecideEnrollmentMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, status }) => decideEnrollment(id, status),
    // Optimistic status badge, per
    // docs/tasks/TCM-16-frontend-course-catalog-enrollment.md step 4: the row
    // flips immediately and the refetch in onSettled confirms it (or onError
    // puts the queue back the way it was).
    onMutate: async ({ id, status }) => {
      await queryClient.cancelQueries({ queryKey: enrollmentsKeys.lists() })
      const previous = queryClient.getQueriesData({ queryKey: enrollmentsKeys.lists() })

      queryClient.setQueriesData({ queryKey: enrollmentsKeys.lists() }, (page) => {
        if (!page) return page
        return {
          ...page,
          content: page.content.map((enrollment) =>
            enrollment.id === id ? { ...enrollment, status } : enrollment,
          ),
        }
      })

      return { previous }
    },
    onError: (error, _variables, context) => {
      context?.previous?.forEach(([queryKey, page]) => queryClient.setQueryData(queryKey, page))
      toast.error(errorMessage(error, 'Failed to update enrollment'))
    },
    onSuccess: (data) => toast.success(data.status === 'APPROVED' ? 'Enrollment approved' : 'Enrollment rejected'),
    onSettled: () => invalidateAffected(queryClient),
  })
}

export function useCancelEnrollmentMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: cancelEnrollment,
    onSuccess: () => {
      invalidateAffected(queryClient)
      toast.success('Enrollment cancelled')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to cancel enrollment')),
  })
}
