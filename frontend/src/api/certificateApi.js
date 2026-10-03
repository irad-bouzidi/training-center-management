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
 * Certificates issued on the courses the signed-in TRAINER teaches.
 *
 * @returns {Promise<object[]>}
 */
export async function listTaughtCertificates() {
  const { data } = await apiClient.get('/certificates/taught')
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
 * The certificate's PDF in one language (`en` or `fr` - every certificate
 * is issued in both). A blob rather than JSON, and fetched through the same
 * client so the Authorization header goes with it - a plain <a href> would
 * arrive unauthenticated.
 *
 * The blob is re-typed as application/pdf whatever the response said, so
 * an object URL made from it is something the browser's PDF viewer will
 * render (the preview's <iframe>) and the OS will open.
 *
 * @returns {Promise<{blob: Blob, filename: string}>}
 */
export async function downloadCertificate(id, lang, fallbackFilename) {
  const response = await apiClient.get(`/certificates/${id}/download`, {
    params: { lang },
    responseType: 'blob',
  })
  return {
    blob: new Blob([response.data], { type: 'application/pdf' }),
    filename: filenameFrom(response.headers['content-disposition']) ?? fallbackFilename,
  }
}

/** `attachment; filename="CERT-2026-000001-en.pdf"` -> `CERT-2026-000001-en.pdf`. */
function filenameFrom(contentDisposition) {
  const match = /filename="?([^";]+)"?/.exec(contentDisposition ?? '')
  return match?.[1]
}
