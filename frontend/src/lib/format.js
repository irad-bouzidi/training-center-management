import i18n from '@/i18n'

// Locale-aware formatting shared by every feature's *Display.js helpers.
// Always formats in the UI's current language (not the browser's), so a
// French UI shows "31 janv. 2026" and "1 284,50" even on an English OS.
// Components re-render on a language change through useTranslation(), which
// re-runs these with the new locale.

export function currentLocale() {
  return i18n.resolvedLanguage ?? i18n.language ?? 'en'
}

export function formatNumber(value, options) {
  return new Intl.NumberFormat(currentLocale(), options).format(value)
}

/** Two fixed decimals - prices and invoice amounts. */
export function formatDecimal(value) {
  return formatNumber(Number(value), { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** A full ISO instant ("2026-01-31T09:00:00Z") -> "Jan 31, 2026" / "31 janv. 2026". */
export function formatDateTimeAsDate(isoString, options = { year: 'numeric', month: 'short', day: 'numeric' }) {
  return new Date(isoString).toLocaleDateString(currentLocale(), options)
}

/** A calendar date ("2026-01-31", no zone) read as local midnight, so it never shifts a day. */
export function formatLocalDate(isoDate, options = { year: 'numeric', month: 'short', day: 'numeric' }) {
  return new Date(`${isoDate}T00:00:00`).toLocaleDateString(currentLocale(), options)
}

export function formatTime(date, options = { hour: '2-digit', minute: '2-digit' }) {
  return new Date(date).toLocaleTimeString(currentLocale(), options)
}
