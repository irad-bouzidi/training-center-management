import { apiClient } from './client'

/**
 * A course's gradebook: every APPROVED-enrolled student with their results
 * and weighted average, graded or not. For the course's trainer or an ADMIN.
 *
 * @returns {Promise<{courseId: string, courseCode: string, courseName: string, students: object[]}>}
 */
export async function getCourseGradebook(courseId) {
  const { data } = await apiClient.get(`/courses/${courseId}/grades`)
  return data
}

/**
 * A student's results, across every course or narrowed to one. Readable by
 * the student themselves, the named course's trainer, or an ADMIN.
 *
 * @returns {Promise<{grades: object[], weightedAverage: number|null}>}
 */
export async function getStudentGrades(studentId, params) {
  const { data } = await apiClient.get(`/students/${studentId}/grades`, { params })
  return data
}

/** @param {{studentId: string, courseId: string, assessmentType: string, title: string, score: string, maxScore: string, weight: string, comments?: string}} payload */
export async function createGrade(payload) {
  const { data } = await apiClient.post('/grades', payload)
  return data
}

/** Same payload as createGrade's; studentId/courseId are fixed server-side. */
export async function updateGrade(id, payload) {
  const { data } = await apiClient.put(`/grades/${id}`, payload)
  return data
}

export async function deleteGrade(id) {
  await apiClient.delete(`/grades/${id}`)
}
