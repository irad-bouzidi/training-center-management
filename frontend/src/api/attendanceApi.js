import { apiClient } from './client'

/**
 * A session's roster: the session itself plus one row per APPROVED-enrolled
 * student, each with their current mark or `status: null` when nobody has
 * marked them yet. Readable by an ADMIN or the session's assigned trainer -
 * anyone else gets a 403 (see AttendanceService#getRoster).
 *
 * @returns {Promise<{session: object, entries: object[]}>}
 */
export async function getSessionRoster(sessionId) {
  const { data } = await apiClient.get(`/sessions/${sessionId}/attendance`)
  return data
}

/**
 * Marks a whole roster in one request. A student already marked has their
 * record corrected rather than duplicated, and students left out of
 * `entries` keep whatever they had.
 *
 * @param {{studentId: string, status: 'PRESENT'|'ABSENT'|'LATE'}[]} entries
 * @returns {Promise<object[]>} the resulting records
 */
export async function markAttendance(sessionId, entries) {
  const { data } = await apiClient.post(`/sessions/${sessionId}/attendance`, { entries })
  return data
}

/**
 * Per-student tallies across every session of a course, for an ADMIN or a
 * trainer of that course.
 *
 * @returns {Promise<{courseId: string, courseCode: string, courseName: string, sessionCount: number, students: object[]}>}
 */
export async function getCourseAttendanceReport(courseId) {
  const { data } = await apiClient.get(`/courses/${courseId}/attendance-report`)
  return data
}
