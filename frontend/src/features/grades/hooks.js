import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { createGrade, deleteGrade, getCourseGradebook, getStudentGrades, updateGrade } from '@/api/gradeApi'
import { studentsKeys } from '@/features/students/hooks'
import { apiErrorMessage } from '@/api/serverErrors'

export const gradesKeys = {
  all: ['grades'],
  gradebooks: () => [...gradesKeys.all, 'gradebook'],
  gradebook: (courseId) => [...gradesKeys.gradebooks(), courseId],
  forStudents: () => [...gradesKeys.all, 'student'],
  forStudent: (studentId, params) => [...gradesKeys.forStudents(), studentId, params],
}

export function useCourseGradebookQuery(courseId, options) {
  return useQuery({
    queryKey: gradesKeys.gradebook(courseId),
    queryFn: () => getCourseGradebook(courseId),
    enabled: Boolean(courseId),
    ...options,
  })
}

export function useStudentGradesQuery(studentId, params, options) {
  return useQuery({
    queryKey: gradesKeys.forStudent(studentId, params),
    queryFn: () => getStudentGrades(studentId, params),
    enabled: Boolean(studentId),
    ...options,
  })
}

/**
 * A grade changing moves the gradebook, the student's own view and the
 * overallGrade on their summary - so all three go stale together.
 */
function invalidateAffected(queryClient) {
  queryClient.invalidateQueries({ queryKey: gradesKeys.all })
  queryClient.invalidateQueries({ queryKey: studentsKeys.all })
}

/**
 * Create/update deliberately have no error toast: both are submitted from
 * GradeFormDialog, which stays open on failure and shows the server's
 * message inline, next to the fields it's about.
 */
export function useCreateGradeMutation() {
  const { t } = useTranslation('grades')
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createGrade,
    onSuccess: () => {
      invalidateAffected(queryClient)
      toast.success(t('toasts.created'))
    },
  })
}

export function useUpdateGradeMutation() {
  const { t } = useTranslation('grades')
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, ...payload }) => updateGrade(id, payload),
    onSuccess: () => {
      invalidateAffected(queryClient)
      toast.success(t('toasts.updated'))
    },
  })
}

export function useDeleteGradeMutation() {
  const { t } = useTranslation('grades')
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deleteGrade,
    onSuccess: () => {
      invalidateAffected(queryClient)
      toast.success(t('toasts.deleted'))
    },
    onError: (error) => toast.error(apiErrorMessage(error, t('toasts.deleteFailed'))),
  })
}
