import i18n from '@/i18n'
import { formatDecimal, formatNumber } from '@/lib/format'
import { parseServerMessage } from './serverErrorRules'

/**
 * Backend error messages in the UI's language (TCM-32).
 *
 * The backend answers every error with docs/PLAN.md §6's ApiError, whose
 * `message` is English prose with no machine-readable code. apiClient asks
 * for English explicitly (Accept-Language: en - otherwise Spring would
 * localize the bean-validation part of it after the browser's language), and
 * its response interceptor runs the message through parseServerMessage()
 * once, attaching the result to the error as `error.serverError`. Nothing is
 * translated at that point: apiErrorMessage() does it whenever it is called,
 * so a message kept on screen follows a later language switch.
 *
 * The rules live in serverErrorRules.js, one per message the backend can
 * send. scripts/check-server-errors.mjs fails when the backend gains a
 * message no rule recognizes, so the two cannot drift silently.
 */

/** Localizes a param a rule tagged ("amount:250.00", "field:email"…). */
function localizeParam(value, t) {
  if (typeof value !== 'string') {
    return value
  }
  const [, kind, raw] = value.match(/^(field|amount|percent|number|enrollmentStatus):(.*)$/) ?? []
  switch (kind) {
    case 'field': {
      // "entries[0].status" is labelled by its last segment.
      const name = raw.replace(/\[\d+\]/g, '').split('.').pop()
      return t(`serverErrors:fields.${name}`, { defaultValue: raw })
    }
    case 'amount':
      return formatDecimal(raw)
    case 'percent':
      return formatNumber(Number(raw) / 100, { style: 'percent', maximumFractionDigits: 1 })
    case 'number':
      return formatNumber(Number(raw))
    case 'enrollmentStatus':
      return t(`common:enums.enrollmentStatus.${raw}`, { defaultValue: raw }).toLowerCase()
    default:
      return value
  }
}

function localizeParams(params, t) {
  return Object.fromEntries(Object.entries(params).map(([name, value]) => [name, localizeParam(value, t)]))
}

/** What parseServerMessage() recognized, in the current language. */
export function translateServerError(parsed, t = i18n.t.bind(i18n)) {
  if (parsed.key === 'validation') {
    return parsed.violations
      .map(({ field, key, params }) =>
        t('serverErrors:validation.item', {
          field: localizeParam(`field:${field}`, t),
          message: t(`serverErrors:validation.${key}`, localizeParams(params, t)),
        }),
      )
      .join(t('serverErrors:validation.separator'))
  }
  return t(`serverErrors:${parsed.key}`, localizeParams(parsed.params, t))
}

/**
 * The message to show for a failed request, in the UI's current language.
 *
 * - A recognized backend message is translated.
 * - A message no rule knows is shown as sent in English, and replaced by
 *   `fallback` in any other language (better an accurate generic line than
 *   a foreign-language one); scripts/check-server-errors.mjs keeps this case
 *   to messages the backend doesn't have yet.
 * - No message at all (network failure) gives `fallback`.
 */
export function apiErrorMessage(error, fallback = null) {
  const parsed = error?.serverError ?? parseServerMessage(error?.response?.data?.message)
  if (!parsed) {
    return fallback
  }
  if (parsed.raw !== undefined) {
    return (i18n.resolvedLanguage ?? 'en') === 'en' || !fallback ? parsed.raw : fallback
  }
  return translateServerError(parsed)
}
