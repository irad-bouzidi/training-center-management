import { CheckCircle2, QrCode } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAuth } from '@/context/AuthContext'
import { formatSessionDate } from '@/features/schedule/scheduleDisplay'
import { formatTime } from '@/lib/format'
import { parseCheckInUrl, parseShortLinkUrl, QrScanner } from './QrScanner'
import { checkInFailure, useQrCheckInMutation } from './qrHooks'

/**
 * What the paste box holds, as a check-in: the short link shown under the QR
 * code (`/s/…`, opened as-is - the backend redirects it back here with the
 * full link), the full check-in link itself (which carries its own session -
 * so either works at a bare /attend), or just the token when the page was
 * opened on the session's own link. Path-only links (/s/…,
 * /attend/…?token=…) are accepted too.
 *
 * @returns {{shortLink: string}|{sessionId: string, token: string}|null}
 */
function parsePasted(text, sessionIdFromPath) {
  const trimmed = text.trim()
  if (!trimmed) {
    return null
  }

  const asUrl = trimmed.startsWith('/') ? `${window.location.origin}${trimmed}` : trimmed
  const shortLink = parseShortLinkUrl(asUrl)
  if (shortLink) {
    return { shortLink }
  }
  const fromLink = parseCheckInUrl(asUrl)
  if (fromLink) {
    return fromLink
  }

  // Anything else is taken as a bare token - unless it plainly is a link
  // (just not a valid check-in one), which no session would accept.
  return sessionIdFromPath && !trimmed.includes('/') ? { sessionId: sessionIdFromPath, token: trimmed } : null
}

/** "Marked present for Java Fundamentals — Mon, 2 Mar 2026", degrading to
 * whatever of the course and date the response carries. */
function checkedInSummary(record, t) {
  const parts = [record.courseName, record.sessionDate && formatSessionDate(record.sessionDate)].filter(Boolean)
  return parts.length > 0
    ? t('checkIn.summaryWithDetails', { details: parts.join(' — ') })
    : t('checkIn.summary')
}

/**
 * Where a scanned QR code lands (/attend/:sessionId?token=…), and where a
 * student can scan one from inside the app (/attend) - see
 * docs/tasks/TCM-28-frontend-qr-attendance.md step 4.
 *
 * Arriving with a token in the URL checks in straight away: the student has
 * already acted by pointing their camera at the code, and asking them to
 * press another button would be asking twice. Without one, the camera opens,
 * with a paste box for devices that won't give it up - it takes the full
 * check-in link (all a bare /attend has to go on), or just the code when
 * the session's own link was opened.
 *
 * A student who scans while signed out comes back here after logging in:
 * LoginPage returns them to ProtectedRoute's `from`, token and all.
 */
export function QrCheckinPage() {
  const { t } = useTranslation('attendance')
  const { sessionId: sessionIdFromPath } = useParams()
  const [searchParams] = useSearchParams()
  const { user } = useAuth()
  const checkIn = useQrCheckInMutation()
  const { mutate } = checkIn
  const [manualInput, setManualInput] = useState('')

  const tokenFromUrl = searchParams.get('token')
  const isStudent = user.role === 'STUDENT'

  const submit = useCallback((scanned) => mutate(scanned), [mutate])
  // A short link is opened rather than posted: the backend redirects it back
  // here on the full link, which checks in on arrival.
  const followShortLink = useCallback((link) => window.location.assign(link), [])
  const pasted = parsePasted(manualInput, sessionIdFromPath)

  // A code in the URL is a scan that has already happened.
  useEffect(() => {
    if (isStudent && sessionIdFromPath && tokenFromUrl) {
      submit({ sessionId: sessionIdFromPath, token: tokenFromUrl })
    }
  }, [isStudent, sessionIdFromPath, tokenFromUrl, submit])

  if (!isStudent) {
    return (
      <Card className="mx-auto mt-10 max-w-md">
        <CardHeader>
          <CardTitle>{t('checkIn.notStudentTitle')}</CardTitle>
          <CardDescription>{t('checkIn.notStudentDescription')}</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild variant="outline">
            <Link to={`/${user.role.toLowerCase()}/schedule`}>{t('checkIn.goToSchedule')}</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  if (checkIn.isSuccess) {
    const record = checkIn.data

    return (
      <Card className="mx-auto mt-10 max-w-md">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <CheckCircle2 className="text-primary" />
            {t('checkIn.successTitle')}
          </CardTitle>
          <CardDescription>{checkedInSummary(record, t)}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-2 text-sm">
          <p className="text-muted-foreground">
            {t('checkIn.recordedAt', { time: formatTime(record.markedAt) })}
          </p>
          <Button asChild variant="outline" size="sm">
            <Link to="/student/schedule">{t('checkIn.backToSchedule')}</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const failure = checkIn.isError ? checkInFailure(checkIn.error) : null

  return (
    <Card className="mx-auto mt-10 max-w-md">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <QrCode />
          {t('checkIn.title')}
        </CardTitle>
        <CardDescription>
          {checkIn.isPending ? t('checkIn.pending') : t('checkIn.instructions')}
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-4">
        {failure && (
          <div className="rounded-md border border-destructive/40 p-3">
            <p className="text-sm font-medium text-destructive">{failure.title}</p>
            <p className="text-sm text-muted-foreground">{failure.detail}</p>
          </div>
        )}

        {!checkIn.isPending && <QrScanner onScan={submit} onShortLink={followShortLink} />}

        <div className="space-y-2">
          <Label htmlFor="manualInput">{t('checkIn.manualLabel')}</Label>
          <div className="flex gap-2">
            <Input
              id="manualInput"
              value={manualInput}
              placeholder={
                sessionIdFromPath ? t('checkIn.manualPlaceholderWithSession') : t('checkIn.manualPlaceholder')
              }
              onChange={(event) => setManualInput(event.target.value)}
            />
            <Button disabled={!pasted || checkIn.isPending} onClick={() => (pasted.shortLink ? followShortLink(pasted.shortLink) : submit(pasted))}>
              {t('checkIn.submit')}
            </Button>
          </div>
          {manualInput.trim() && !pasted && (
            <p className="text-xs text-muted-foreground">
              {sessionIdFromPath
                ? t('checkIn.invalidWithSession')
                : t('checkIn.invalid')}
            </p>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
