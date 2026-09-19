/**
 * Browser click-through of every role's core journey (TCM-30), against a
 * stack that is already up. Complements ../../scripts/smoke.sh, which checks
 * the same journeys at the API level: this one checks that they are
 * reachable and readable in the real UI, and that no page logs a console
 * error.
 *
 *   npx playwright install chromium   # the browser, downloaded once
 *   node scripts/ui-smoke.mjs         # from frontend/
 *
 * playwright-core is a devDependency (a few MB, no bundled browsers); the
 * browser itself is downloaded on demand, which is why it is found rather
 * than hardcoded. BASE_URL and CHROME_PATH override the defaults.
 */
import { existsSync, readdirSync } from 'node:fs'
import { homedir } from 'node:os'
import { join } from 'node:path'
import { chromium } from 'playwright-core'

const BASE = process.env.BASE_URL ?? 'http://localhost:5173'

/** The newest chromium `playwright install` has put in its cache. */
function findChromium() {
  if (process.env.CHROME_PATH) {
    return process.env.CHROME_PATH
  }
  const cache = join(homedir(), '.cache', 'ms-playwright')
  if (!existsSync(cache)) {
    return undefined
  }
  const builds = readdirSync(cache)
    .filter((name) => name.startsWith('chromium-'))
    .sort((a, b) => Number(b.split('-')[1]) - Number(a.split('-')[1]))
  for (const build of builds) {
    for (const exe of ['chrome-linux64/chrome', 'chrome-linux/chrome', 'chrome-mac/Chromium.app/Contents/MacOS/Chromium']) {
      const candidate = join(cache, build, exe)
      if (existsSync(candidate)) {
        return candidate
      }
    }
  }
  return undefined
}

const EXE = findChromium()
if (!EXE) {
  console.error('No chromium found. Run: npx playwright install chromium')
  process.exit(2)
}
const PW = 'ChangeMe123!'
let pass = 0, fail = 0
const check = (name, ok, detail = '') => {
  console.log(`  ${ok ? 'PASS' : 'FAIL'}  ${name}${ok ? '' : ' -> ' + detail}`)
  if (ok) {
    pass++
  } else {
    fail++
  }
}

/** The pages render their figures once the query resolves, which is after
 *  networkidle - wait for the text itself rather than for the network. */
const bodyAfter = async (page, text) => {
  await page.waitForSelector(`text=${text}`, { timeout: 15000 }).catch(() => {})
  return page.textContent('body')
}

const browser = await chromium.launch({
  ...(EXE ? { executablePath: EXE } : {}),
  args: ['--no-sandbox'],
})

async function signIn(email) {
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } })
  const errors = []
  page.on('console', (m) => m.type() === 'error' && errors.push(m.text()))
  page.on('pageerror', (e) => errors.push(String(e)))
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' })
  await page.fill('input[type="email"], #email', email)
  await page.fill('input[type="password"], #password', PW)
  await page.click('button[type="submit"]')
  // The SPA navigates client-side, so there is no load event to wait on -
  // wait for the role home the login redirected to.
  await page.waitForURL(/\/(admin|trainer|student)/, { timeout: 15000 })
  await page.waitForLoadState('networkidle')
  return { page, errors }
}

// ---- Admin ----------------------------------------------------------------
{
  const { page, errors } = await signIn('admin@tcm.local')
  check('admin lands on the dashboard', page.url().includes('/admin'), page.url())
  const body = await bodyAfter(page, 'Outstanding balance')
  check('dashboard shows live figures', /Active students/.test(body) && /Outstanding balance/.test(body))
  check('figures are real, not placeholders', !/placeholder/i.test(body))
  await page.screenshot({ path: 'shot-admin-dashboard.png' })

  await page.goto(`${BASE}/admin/payments`, { waitUntil: 'networkidle' })
  const payments = await page.textContent('body')
  check('payments ledger lists seeded invoices', /Overdue|Partial|Paid/.test(payments))
  await page.screenshot({ path: 'shot-admin-payments.png' })

  await page.goto(`${BASE}/admin/students`, { waitUntil: 'networkidle' })
  await page.click('text=Sofia')
  await page.waitForLoadState('networkidle')
  const summary = await bodyAfter(page, 'Attendance Rate')
  check('student summary opens with real stats', /Attendance Rate/.test(summary) && /100%/.test(summary))
  for (const tab of ['Attendance', 'Grades', 'Payments', 'Certificates']) {
    await page.click(`button[role="tab"]:has-text("${tab}")`)
    await page.waitForTimeout(600)
    const text = await page.textContent('body')
    check(`${tab} tab renders real content`, !/coming soon/i.test(text) && !/TCM-\d+/.test(text))
  }
  await page.screenshot({ path: 'shot-admin-student-certificates.png' })
  check('admin pages raise no console errors', errors.length === 0, errors.slice(0, 2).join(' | '))
  await page.close()
}

// ---- Trainer --------------------------------------------------------------
{
  const { page, errors } = await signIn('tina.trainer@tcm.local')
  check('trainer lands on their dashboard', page.url().includes('/trainer'), page.url())
  const body = await bodyAfter(page, 'Sessions to mark')
  check('trainer tiles are scoped to them', /My courses/.test(body) && /Sessions to mark/.test(body))

  await page.goto(`${BASE}/trainer/schedule`, { waitUntil: 'networkidle' })
  const schedule = await page.textContent('body')
  check('schedule shows their sessions', /Java Fundamentals/.test(schedule) && /Room A/.test(schedule))
  await page.screenshot({ path: 'shot-trainer-schedule.png' })

  // Take attendance on the upcoming Java session seeded by TCM-30.
  await page.goto(`${BASE}/trainer/sessions/55555555-5555-4555-8555-555555555503/attendance`, {
    waitUntil: 'networkidle',
  })
  const roster = await page.textContent('body')
  check('roster lists approved students', /Sam Student/.test(roster) && /Present/.test(roster))
  await page.click('button:has-text("Mark all present")')
  const save = page.locator('button:has-text("Save")')
  if (await save.isEnabled()) {
    await save.click()
    await page.waitForTimeout(1500)
    check('saving attendance succeeds', /saved/i.test(await page.textContent('body')))
  } else {
    // A re-run: everyone is already present, so there is nothing to save -
    // which is itself the previous run's save having persisted.
    check('attendance persisted from an earlier run', /of \d+ marked/.test(await page.textContent('body')))
  }

  await page.click('button:has-text("Show QR")')
  await page.waitForTimeout(1500)
  const qrVisible = await page.locator('img[alt*="QR code"]').isVisible()
  check('QR code renders on screen', qrVisible)
  check('QR dialog counts down', /Expires in/.test(await page.textContent('body')))
  await page.screenshot({ path: 'shot-trainer-qr.png' })
  check('trainer pages raise no console errors', errors.length === 0, errors.slice(0, 2).join(' | '))
  await page.close()
}

// ---- Student --------------------------------------------------------------
{
  const { page, errors } = await signIn('sofia.benali@tcm.local')
  check('student lands on their dashboard', page.url().includes('/student'), page.url())
  check('student sees their own figures', /Overall grade/.test(await bodyAfter(page, 'Overall grade')))

  await page.goto(`${BASE}/student/grades`, { waitUntil: 'networkidle' })
  check('grades are grouped by course', /Java Fundamentals/.test(await page.textContent('body')))

  await page.goto(`${BASE}/student/certificates`, { waitUntil: 'networkidle' })
  const certs = await page.textContent('body')
  check('the issued certificate is listed', /CERT-/.test(certs))
  const download = page.waitForEvent('download', { timeout: 15000 })
  await page.click('button:has-text("Download PDF")')
  const file = await download
  check('downloading gives a .pdf file', file.suggestedFilename().endsWith('.pdf'), file.suggestedFilename())
  await page.screenshot({ path: 'shot-student-certificates.png' })

  await page.goto(`${BASE}/attend`, { waitUntil: 'networkidle' })
  check('check-in page opens for a student', /Check in/.test(await page.textContent('body')))
  check('student pages raise no console errors', errors.length === 0, errors.slice(0, 2).join(' | '))
  await page.close()
}

await browser.close()
console.log(`\nPASS=${pass} FAIL=${fail}`)
process.exit(fail === 0 ? 0 : 1)
