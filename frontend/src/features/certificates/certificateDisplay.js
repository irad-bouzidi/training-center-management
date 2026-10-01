import i18n from '@/i18n'
import { formatDateTimeAsDate } from '@/lib/format'

/** "2 March 2026" / "2 mars 2026" - when a certificate was issued. */
export function formatIssuedAt(isoInstant) {
  return formatDateTimeAsDate(isoInstant, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

/**
 * Why a course can't be certified yet, judged on what the client can see -
 * the enrollment's status. The attendance half of the rule
 * (CertificateEligibilityService) is only known to the server, and comes
 * back as the message on a refused generate.
 *
 * Translated in the UI's current language; callers re-render on a language
 * change through their own useTranslation().
 *
 * @returns {string|null} null when it's worth trying.
 */
export function blockingReason(enrollment) {
  if (enrollment.status === 'COMPLETED') {
    return null
  }
  return i18n.t('certificates:blockingReason', {
    status: i18n.t(`common:enums.enrollmentStatus.${enrollment.status}`).toLowerCase(),
  })
}
