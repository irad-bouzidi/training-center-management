import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { generateSessionQr, qrCheckIn } from '@/api/qrAttendanceApi'
import i18n from '@/i18n'
import { attendanceKeys } from './hooks'
import { apiErrorMessage } from '@/api/serverErrors'

/**
 * A fresh code for a session. Deliberately a mutation rather than a query:
 * asking for one *replaces* the session's current code, which is not
 * something a refetch should do behind the trainer's back.
 */
export function useGenerateSessionQrMutation(sessionId) {
  const { t } = useTranslation('attendance')

  return useMutation({
    mutationFn: () => generateSessionQr(sessionId),
    onError: (error) => toast.error(apiErrorMessage(error, t('toasts.qrFailed'))),
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
 *
 * Not a hook, so it translates through the i18n instance directly; it runs
 * during QrCheckinPage's render, which re-renders on a language change.
 */
export function checkInFailure(error) {
  const t = i18n.getFixedT(null, 'attendance', 'failure')
  const status = error.response?.status
  // The backend's own reason, translated, when it sent one.
  const message = apiErrorMessage(error)

  if (status === 410) {
    return { title: t('expiredTitle'), detail: message ?? t('expiredDetail') }
  }
  if (status === 403) {
    return { title: t('forbiddenTitle'), detail: message ?? t('forbiddenDetail') }
  }
  if (status === 400) {
    return { title: t('wrongSessionTitle'), detail: message ?? t('wrongSessionDetail') }
  }
  return { title: t('genericTitle'), detail: message ?? t('genericDetail') }
}
