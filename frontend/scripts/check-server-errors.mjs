/**
 * Every error message the backend can send must be one the frontend knows how
 * to translate (TCM-32). Scans the backend's Java source for the messages its
 * exceptions carry, builds a sample of each (concatenated values replaced by
 * realistic stand-ins), and checks it against src/api/serverErrorRules.js,
 * that every rule has an en and fr translation, and that each message renders
 * cleanly in both languages through the app's own translator (loaded with
 * Vite, as the app would be).
 *
 *   node scripts/check-server-errors.mjs   # from frontend/; exits 1 on a gap
 *
 * A message is left out when the client never sees it: AccessDeniedException
 * and UsernameNotFoundException texts are replaced by GlobalExceptionHandler's
 * generic ones.
 */
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { RULES, CONSTRAINT_RULES, parseServerMessage } from '../src/api/serverErrorRules.js'

const BACKEND = new URL('../../backend/src/main/java/', import.meta.url).pathname
const SENT = ['BadRequestException', 'ConflictException', 'GoneException', 'ResourceNotFoundException']

function javaFiles(dir) {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name)
    return statSync(path).isDirectory() ? javaFiles(path) : path.endsWith('.java') ? [path] : []
  })
}

/** The argument list of a call starting at `open` (the index of its "("). */
function callArgs(source, open) {
  let depth = 0
  let inString = false
  for (let i = open; i < source.length; i++) {
    const c = source[i]
    if (inString) {
      if (c === '\\') i++
      else if (c === '"') inString = false
    } else if (c === '"') inString = true
    else if (c === '(') depth++
    else if (c === ')' && --depth === 0) return source.slice(open + 1, i)
  }
  return null
}

/** A stand-in for a concatenated Java expression, by what it's named. */
function sample(expression) {
  const e = expression.trim()
  if (/[Ii]d\b|getId\(\)/.test(e)) return '0b5c7a1e-2f3d-4c5b-8a9e-1d2c3b4a5f6e'
  if (/Status\(\)/.test(e)) return 'APPROVED'
  if (/CertificateNumber\(\)/.test(e)) return 'TCM-2026-0001'
  if (/outstanding|[Rr]ate/.test(e)) return '62.5'
  if (/getName\(\)/.test(e)) return 'id'
  return null
}

/** "No course with id " + courseId -> "No course with id 0b5c…", or why not. */
function render(expression) {
  const parts = []
  const unknown = []
  // Split on + outside string literals.
  const tokens = expression.match(/"(?:[^"\\]|\\.)*"|[^+"]+/g) ?? []
  for (const token of tokens) {
    const t = token.trim()
    if (!t) continue
    if (t.startsWith('"')) parts.push(JSON.parse(t))
    else {
      const value = sample(t)
      if (value === null) unknown.push(t)
      else parts.push(value)
    }
  }
  return { text: parts.join(''), unknown }
}

const found = []
for (const file of javaFiles(BACKEND)) {
  const source = readFileSync(file, 'utf8')
  const short = file.slice(BACKEND.length)
  for (const match of source.matchAll(new RegExp(`new (${SENT.join('|')})\\(`, 'g'))) {
    const args = callArgs(source, match.index + match[0].length - 1)
    if (args === null || !args.includes('"')) continue // e.g. new BadRequestException(reason)
    found.push({ where: short, ...render(args) })
  }
  // Messages built elsewhere and thrown as a variable (certificate eligibility).
  if (/ineligibilityReason/.test(source) && /EligibilityService/.test(short)) {
    for (const match of source.matchAll(/return ("(?:[^"\\]|\\.)*"(?:\s*\+\s*[^;]+)?);/g)) {
      found.push({ where: short, ...render(match[1].replace(/\s*\n\s*/g, ' ')) })
    }
  }
  // GlobalExceptionHandler / SecurityConfig's fixed messages.
  if (/GlobalExceptionHandler|SecurityConfig/.test(short)) {
    for (const match of source.matchAll(/(?:build\(HttpStatus\.\w+,|writeError\([^,]*,)\s*("(?:[^"\\]|\\.)*"(?:\s*\+[^,]+)?)/g)) {
      found.push({ where: short, ...render(match[1]) })
    }
    for (const match of source.matchAll(/"(Validation failed|Authentication is required[^"]*)"/g)) {
      found.push({ where: short, text: match[1], unknown: [] })
    }
  }
}

// What handleValidation builds out of bean-validation defaults and the
// DTOs' own `message =` texts.
const VALIDATION_SAMPLES = [
  'email: must not be blank',
  'role: must not be null; email: must be a well-formed email address',
  'title: size must be between 0 and 200',
  'capacity: must be greater than 0',
  'price: must be greater than or equal to 0',
  'weight: must be greater than zero; score: must not be negative',
  'entries: must not be empty',
  'entries[0].status: must not be null',
  'maxScore: must be greater than or equal to 0.01',
]

let failures = 0
for (const { where, text, unknown } of found) {
  if (unknown.length) {
    console.log(`FAIL  ${where}: can't build a sample for ${unknown.join(', ')} - teach sample() about it`)
    failures++
  } else if (parseServerMessage(text)?.raw !== undefined) {
    console.log(`FAIL  ${where}: no rule recognizes "${text}"`)
    failures++
  }
}
for (const text of VALIDATION_SAMPLES) {
  if (parseServerMessage(text)?.key !== 'validation') {
    console.log(`FAIL  validation sample not recognized: "${text}"`)
    failures++
  }
}

// Every rule's key translated in both languages.
const locales = Object.fromEntries(
  ['en', 'fr'].map((lng) => [lng, JSON.parse(readFileSync(new URL(`../src/locales/${lng}.json`, import.meta.url))).serverErrors]),
)
const keys = [...RULES.map(([, key]) => key), ...CONSTRAINT_RULES.map(([, key]) => `validation.${key}`)]
for (const lng of Object.keys(locales)) {
  for (const key of new Set(keys)) {
    if (key.split('.').reduce((node, part) => node?.[part], locales[lng]) === undefined) {
      console.log(`FAIL  ${lng}.json has no serverErrors.${key}`)
      failures++
    }
  }
}

// Render every message through the app's own translator, in each language:
// no placeholder may be left unfilled and no key may be missing.
globalThis.document = { documentElement: {}, title: '' }
globalThis.localStorage = { getItem: () => null, setItem: () => {}, removeItem: () => {} }
const { createServer } = await import('vite')
const vite = await createServer({ server: { middlewareMode: true }, appType: 'custom', logLevel: 'error' })
const { default: i18n } = await vite.ssrLoadModule('/src/i18n/index.js')
const { apiErrorMessage } = await vite.ssrLoadModule('/src/api/serverErrors.js')
for (const lng of ['en', 'fr']) {
  await i18n.changeLanguage(lng)
  for (const text of [...found.filter((f) => !f.unknown.length).map((f) => f.text), ...VALIDATION_SAMPLES]) {
    const shown = apiErrorMessage({ serverError: parseServerMessage(text) }, '<fallback>')
    if (/{{|serverErrors[:.]|<fallback>/.test(shown)) {
      console.log(`FAIL  ${lng}: "${text}" renders as "${shown}"`)
      failures++
    }
  }
}
await vite.close()

console.log(`${found.length} backend messages, ${VALIDATION_SAMPLES.length} validation samples, ${new Set(keys).size} rule keys checked - ${failures ? `${failures} FAILED` : 'all recognized'}`)
process.exit(failures ? 1 : 0)
