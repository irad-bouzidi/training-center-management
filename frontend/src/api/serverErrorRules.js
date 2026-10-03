/**
 * Recognizes the backend's English error messages (TCM-32). Kept free of
 * imports - no i18n, no Vite aliases - so scripts/check-server-errors.mjs can
 * run it under plain Node against the messages found in the backend source.
 * Translation happens in serverErrors.js.
 *
 * Each rule mirrors one message the backend can send; the comment above a
 * group names the backend package it comes from.
 */

const UUID = '[0-9a-fA-F-]{36}'
const NUMBER = '(-?\\d+(?:\\.\\d+)?)'

/**
 * [pattern, key under `serverErrors:`, params from the match]. Param values
 * prefixed by a marker are localized on render: see localizeParam().
 */
export const RULES = [
  // common/GlobalExceptionHandler
  [/^Validation failed$/, 'validationFailed'],
  [/^Request body is missing or malformed$/, 'malformedRequest'],
  [/^'(\w+)' is not a valid value$/, 'invalidValue', (m) => ({ field: `field:${m[1]}` })],
  [/^Invalid email or password$/, 'invalidCredentials'],
  [/^You do not have permission to perform this action$/, 'forbidden'],
  [/^An unexpected error occurred$/, 'unexpected'],
  // config/SecurityConfig authenticationEntryPoint
  [/^Authentication is required to access this resource$/, 'authenticationRequired'],
  // EntityNotFoundException from JPA references (getReferenceById)
  [/^Unable to find [\w.]+ with id .+$/, 'notFound'],

  // Not-found lookups, any service
  [new RegExp(`^No course with id (${UUID})$`), 'noCourse', (m) => ({ id: m[1] })],
  [new RegExp(`^No user with id (${UUID})$`), 'noUser', (m) => ({ id: m[1] })],
  [new RegExp(`^No session with id (${UUID})$`), 'noSession', (m) => ({ id: m[1] })],
  [new RegExp(`^No payment with id (${UUID})$`), 'noPayment', (m) => ({ id: m[1] })],
  [new RegExp(`^No grade with id (${UUID})$`), 'noGrade', (m) => ({ id: m[1] })],
  [new RegExp(`^No enrollment with id (${UUID})$`), 'noEnrollment', (m) => ({ id: m[1] })],
  [new RegExp(`^No certificate with id (${UUID})$`), 'noCertificate', (m) => ({ id: m[1] })],

  // user/
  [/^A user with this email already exists$/, 'emailTaken'],
  [/^password is required$/, 'passwordRequired'],
  [new RegExp(`^User (${UUID}) is not a student$`), 'notAStudent', (m) => ({ id: m[1] })],

  // course/
  [/^A course with this code already exists$/, 'courseCodeTaken'],
  [/^Cannot delete a course that has enrollments$/, 'courseHasEnrollments'],
  [/^primaryTrainerId must reference a user with role TRAINER$/, 'primaryTrainerNotTrainer'],

  // enrollment/
  [/^Course is not open for enrollment$/, 'courseNotOpen'],
  [/^Course has reached its capacity$/, 'courseFull'],
  [/^Student is already enrolled in this course$/, 'alreadyEnrolled'],
  [/^studentId is required when an administrator registers a student$/, 'studentIdRequired'],
  [/^studentId must reference a user with role STUDENT$/, 'studentNotStudent'],
  [/^decision must be APPROVED or REJECTED$/, 'invalidDecision'],
  [/^Only PENDING enrollments can be approved or rejected$/, 'onlyPendingDecided'],
  [/^Only APPROVED enrollments can be marked completed$/, 'onlyApprovedCompleted'],
  [/^Only PENDING or APPROVED enrollments can be cancelled$/, 'onlyPendingOrApprovedCancelled'],

  // schedule/
  [/^trainerId must reference a user with role TRAINER$/, 'trainerNotTrainer'],
  [/^endTime must be after startTime$/, 'endBeforeStart'],
  [/^status must be CANCELLED or COMPLETED$/, 'invalidSessionStatus'],
  [/^Only SCHEDULED sessions can be rescheduled$/, 'onlyScheduledRescheduled'],
  [/^Only SCHEDULED sessions can be marked completed$/, 'onlyScheduledCompleted'],
  [/^Only SCHEDULED sessions can be cancelled$/, 'onlyScheduledCancelled'],
  [/^That trainer and classroom are both already booked for an overlapping session$/, 'trainerAndClassroomBooked'],
  [/^That trainer is already booked for an overlapping session$/, 'trainerBooked'],
  [/^That classroom is already booked for an overlapping session$/, 'classroomBooked'],

  // attendance/ (incl. QR check-in)
  [/^Attendance cannot be marked for a cancelled session$/, 'attendanceCancelledSession'],
  [
    new RegExp(`^Student (${UUID}) has no APPROVED or COMPLETED enrollment in this session's course$`),
    'noEnrollmentForSession',
    (m) => ({ id: m[1] }),
  ],
  [/^This session was cancelled, so there is nothing to check in to$/, 'checkInCancelledSession'],
  [/^That QR code is not valid for this session$/, 'qrInvalid'],
  [/^That QR code has expired - ask the trainer to show a fresh one$/, 'qrExpired'],
  [/^That QR code has been replaced by a newer one - ask for the current code$/, 'qrReplaced'],

  // payment/
  [
    new RegExp(`^That payment exceeds the ${NUMBER} still owed on this invoice$`),
    'paymentExceedsOutstanding',
    (m) => ({ amount: `amount:${m[1]}` }),
  ],

  // grade/
  [/^score must not exceed maxScore$/, 'scoreExceedsMax'],
  [
    new RegExp(`^Student (${UUID}) has no APPROVED enrollment in this course$`),
    'noApprovedEnrollment',
    (m) => ({ id: m[1] }),
  ],

  // certificate/ (CertificateServiceImpl, CertificateEligibilityService)
  [
    new RegExp(`^Certificate (\\S+) \\(id (${UUID})\\) already issued to this student for this course$`),
    'certificateAlreadyIssued',
    (m) => ({ number: m[1] }),
  ],
  [/^Unsupported certificate language: (.*)$/, 'certificateUnsupportedLanguage', (m) => ({ lang: m[1] })],
  [/^This student is not enrolled in this course$/, 'certificateNotEnrolled'],
  [
    /^This student's enrollment must be marked COMPLETED before a certificate can be issued \(it is currently (\w+)\)$/,
    'certificateNotCompleted',
    (m) => ({ status: `enrollmentStatus:${m[1]}` }),
  ],
  [
    new RegExp(`^No attendance has been recorded for this student on this course, so the ${NUMBER}% requirement can't be shown to be met$`),
    'certificateNoAttendance',
    (m) => ({ required: `percent:${m[1]}` }),
  ],
  [
    new RegExp(`^Attendance on this course is ${NUMBER}%, below the ${NUMBER}% required for a certificate$`),
    'certificateAttendanceTooLow',
    (m) => ({ rate: `percent:${m[1]}`, required: `percent:${m[2]}` }),
  ],
]

/**
 * Bean-validation messages - Hibernate Validator's defaults and the DTOs'
 * own `message =` - as they come back inside "field: message; …".
 */
export const CONSTRAINT_RULES = [
  [/^must not be blank$/, 'notBlank'],
  [/^must not be null$/, 'notNull'],
  [/^must not be empty$/, 'notBlank'],
  [/^must be a well-formed email address$/, 'email'],
  [/^size must be between 0 and (\d+)$/, 'sizeMax', (m) => ({ max: `number:${m[1]}` })],
  [/^size must be between (\d+) and (\d+)$/, 'size', (m) => ({ min: `number:${m[1]}`, max: `number:${m[2]}` })],
  [/^must be greater than 0$/, 'positive'],
  [/^must be greater than zero$/, 'positive'],
  [/^must be greater than or equal to 0$/, 'nonNegative'],
  [/^must not be negative$/, 'nonNegative'],
  [/^must be greater than or equal to (\S+)$/, 'min', (m) => ({ min: `number:${m[1]}` })],
  [/^must be less than or equal to (\S+)$/, 'max', (m) => ({ max: `number:${m[1]}` })],
]

function matchRules(rules, text) {
  for (const [pattern, key, params] of rules) {
    const match = text.match(pattern)
    if (match) {
      return { key, params: params ? params(match) : {} }
    }
  }
  return null
}

/** GlobalExceptionHandler.handleValidation's "field: message; field: message". */
function parseValidation(message) {
  const parts = message.split('; ').map((part) => {
    const match = part.match(/^([\w.[\]]+): (.+)$/)
    const constraint = match && matchRules(CONSTRAINT_RULES, match[2])
    return constraint ? { field: match[1], ...constraint } : null
  })
  return parts.length && parts.every(Boolean) ? { key: 'validation', violations: parts } : null
}

/**
 * The backend message, recognized: `{ key, params }`, `{ key: 'validation',
 * violations }`, or `{ raw }` for text no rule knows (kept so English users
 * still see it). null when there is no message at all (a network error).
 */
export function parseServerMessage(message) {
  if (typeof message !== 'string' || !message.trim()) {
    return null
  }
  const text = message.trim()
  return matchRules(RULES, text) ?? parseValidation(text) ?? { raw: text }
}
