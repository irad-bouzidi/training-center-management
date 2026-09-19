import { apiClient } from './client'

/**
 * Platform-wide figures for an ADMIN. Read-only aggregation over existing
 * data - the dashboard stores nothing of its own (TCM-29).
 *
 * @returns {Promise<{activeStudents: number, activeTrainers: number, publishedCourses: number, pendingEnrollments: number, upcomingSessions: number, outstandingBalance: string, overdueInvoices: number, averageAttendanceRate: number|null, certificatesThisMonth: number}>}
 */
export async function getAdminSummary() {
  const { data } = await apiClient.get('/dashboard/summary')
  return data
}

/**
 * The signed-in TRAINER's own figures. There is no id to pass, so no one
 * else's.
 *
 * @returns {Promise<{myCourses: number, upcomingSessions: number, sessionsAwaitingAttendance: number, studentsAwaitingGrades: number}>}
 */
export async function getTrainerSummary() {
  const { data } = await apiClient.get('/dashboard/trainer-summary')
  return data
}
