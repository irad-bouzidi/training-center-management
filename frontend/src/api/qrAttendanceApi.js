import { apiClient } from './client'

/**
 * Mints a fresh QR code for a session (the assigned TRAINER or an ADMIN),
 * replacing whatever code it had. The PNG comes back base64-encoded
 * alongside its expiry, so the screen showing it can count down without a
 * second call.
 *
 * @returns {Promise<{token: string, expiresAt: string, checkInUrl: string, imageBase64: string}>}
 */
export async function generateSessionQr(sessionId) {
  const { data } = await apiClient.post(`/sessions/${sessionId}/qr`)
  return data
}

/**
 * Marks the signed-in STUDENT present by their scan. There is no studentId
 * to pass - a scan can only ever check in the person doing it.
 *
 * Rejections are meaningful: 410 for an expired or superseded code, 400 for
 * one that was never this session's, 403 when they aren't on the course.
 */
export async function qrCheckIn({ sessionId, token }) {
  const { data } = await apiClient.post('/attendance/qr-checkin', { sessionId, token })
  return data
}
