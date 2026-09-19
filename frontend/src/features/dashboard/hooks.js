import { useQuery } from '@tanstack/react-query'
import { getAdminSummary, getTrainerSummary } from '@/api/dashboardApi'

export const dashboardKeys = {
  all: ['dashboard'],
  admin: () => [...dashboardKeys.all, 'admin'],
  trainer: () => [...dashboardKeys.all, 'trainer'],
}

export function useAdminSummaryQuery() {
  return useQuery({
    queryKey: dashboardKeys.admin(),
    queryFn: getAdminSummary,
  })
}

export function useTrainerSummaryQuery() {
  return useQuery({
    queryKey: dashboardKeys.trainer(),
    queryFn: getTrainerSummary,
  })
}
