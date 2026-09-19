import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { getCourseAttendanceReport, getSessionRoster, markAttendance } from '@/api/attendanceApi'
import { errorMessage } from '@/features/schedule/hooks'

export const attendanceKeys = {
  all: ['attendance'],
  rosters: () => [...attendanceKeys.all, 'roster'],
  roster: (sessionId) => [...attendanceKeys.rosters(), sessionId],
  reports: () => [...attendanceKeys.all, 'report'],
  report: (courseId) => [...attendanceKeys.reports(), courseId],
}

export function useSessionRosterQuery(sessionId) {
  return useQuery({
    queryKey: attendanceKeys.roster(sessionId),
    queryFn: () => getSessionRoster(sessionId),
    enabled: Boolean(sessionId),
  })
}

export function useCourseAttendanceReportQuery(courseId, options) {
  return useQuery({
    queryKey: attendanceKeys.report(courseId),
    queryFn: () => getCourseAttendanceReport(courseId),
    enabled: Boolean(courseId),
    ...options,
  })
}

/**
 * The same report for several courses at once - what the student summary's
 * attendance tab needs to break a student's record down per course. Each
 * course is its own query, so one a trainer may not report on fails alone
 * rather than blanking the tab.
 */
export function useCourseAttendanceReportQueries(courseIds) {
  return useQueries({
    queries: courseIds.map((courseId) => ({
      queryKey: attendanceKeys.report(courseId),
      queryFn: () => getCourseAttendanceReport(courseId),
      retry: false,
    })),
  })
}

/**
 * Saves a whole roster. Both the roster and every report that counts these
 * marks are invalidated, as is the student summary whose attendanceRate they
 * feed.
 */
export function useMarkAttendanceMutation(sessionId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (entries) => markAttendance(sessionId, entries),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: attendanceKeys.all })
      queryClient.invalidateQueries({ queryKey: ['students'] })
      toast.success('Attendance saved')
    },
    onError: (error) => toast.error(errorMessage(error, 'Failed to save attendance')),
  })
}
