/** "2 March 2026" - when a certificate was issued. */
export function formatIssuedAt(isoInstant) {
  return new Date(isoInstant).toLocaleDateString(undefined, {
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
 * @returns {string|null} null when it's worth trying.
 */
export function blockingReason(enrollment) {
  if (enrollment.status === 'COMPLETED') {
    return null
  }
  return `This enrollment is ${enrollment.status.toLowerCase()}. It must be marked completed before a certificate can be issued.`
}
