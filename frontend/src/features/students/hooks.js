import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getStudentSummary, listStudents } from '@/api/studentApi'

export const studentsKeys = {
  all: ['students'],
  lists: () => [...studentsKeys.all, 'list'],
  list: (params) => [...studentsKeys.lists(), params],
  summaries: () => [...studentsKeys.all, 'summary'],
  summary: (id) => [...studentsKeys.summaries(), id],
}

export function useStudentsQuery(params) {
  return useQuery({
    queryKey: studentsKeys.list(params),
    queryFn: () => listStudents(params),
    // Keeps the current page's rows on screen while the next page loads,
    // instead of the table flashing empty between pages/filter changes.
    placeholderData: keepPreviousData,
  })
}

export function useStudentSummaryQuery(id) {
  return useQuery({
    queryKey: studentsKeys.summary(id),
    queryFn: () => getStudentSummary(id),
    enabled: Boolean(id),
  })
}
