import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { generateSessionQr, qrCheckIn } from '@/api/qrAttendanceApi'
import { attendanceKeys, errorMessage } from './hooks'

/**
 * A fresh code for a session. Deliberately a mutation rather than a query:
 * asking for one *replaces* the session's current code, which is not
 * something a refetch should do behind the trainer's back.
 */
export function useGenerateSessionQrMutation(sessionId) {
  return useMutation({
    mutationFn: () => generateSessionQr(sessionId),
    onError: (error) => toast.error(errorMessage(error, 'Failed to produce a QR code')),
  })
}

/**
 * The student's scan. A successful check-in changes the session's roster, so
 * any attendance view open elsewhere is stale.
 */
export function useQrCheckInMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: qrCheckIn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: attendanceKeys.all }),
  })
}

/**
 * What went wrong with a scan, in the phrasing the person holding the phone
 * needs. The server's own message is preferred - it distinguishes an expired
 * code from a superseded one, which matters to whether they should ask for a
 * new one or just look at the screen again.
 */
export function checkInFailure(error) {
  const status = error.response?.status
  const message = error.response?.data?.message

  if (status === 410) {
    return { title: 'That code is no longer valid', detail: message ?? 'Ask your trainer to show a fresh one.' }
  }
  if (status === 403) {
    return { title: 'You’re not on this course', detail: message ?? 'Check-in is for students enrolled on it.' }
  }
  if (status === 400) {
    return { title: 'That code doesn’t belong to this session', detail: message ?? 'Scan the code on screen again.' }
  }
  return { title: 'Check-in failed', detail: message ?? 'Try again in a moment.' }
}
