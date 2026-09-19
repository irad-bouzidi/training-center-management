import { apiClient } from './client'

/**
 * Certificates issued to one student. A STUDENT may omit `studentId` and
 * gets their own; an ADMIN or TRAINER may ask about anyone.
 *
 * @returns {Promise<object[]>}
 */
export async function listCertificates(studentId) {
  const { data } = await apiClient.get('/certificates', { params: studentId ? { studentId } : undefined })
  return data
}

/**
 * Issues a certificate (ADMIN, or the course's trainer). Rejected with a 400
 * naming the rule the student fails, or a 409 if they already hold one for
 * the course.
 */
export async function generateCertificate(payload) {
  const { data } = await apiClient.post('/certificates/generate', payload)
  return data
}

/**
 * The certificate's PDF. A blob rather than JSON, and fetched through the
 * same client so the Authorization header goes with it - a plain <a href>
 * would arrive unauthenticated.
 *
 * @returns {Promise<{blob: Blob, filename: string}>}
 */
export async function downloadCertificate(id, fallbackFilename) {
  const response = await apiClient.get(`/certificates/${id}/download`, { responseType: 'blob' })
  return {
    blob: response.data,
    filename: filenameFrom(response.headers['content-disposition']) ?? fallbackFilename,
  }
}

/** `attachment; filename="CERT-2026-000001.pdf"` -> `CERT-2026-000001.pdf`. */
function filenameFrom(contentDisposition) {
  const match = /filename="?([^";]+)"?/.exec(contentDisposition ?? '')
  return match?.[1]
}
