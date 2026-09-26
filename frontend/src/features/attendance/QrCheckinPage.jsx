import { CheckCircle2, QrCode } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAuth } from '@/context/AuthContext'
import { formatSessionDate } from '@/features/schedule/scheduleDisplay'
import { parseCheckInUrl, QrScanner } from './QrScanner'
import { checkInFailure, useQrCheckInMutation } from './qrHooks'

/**
 * What the paste box holds, as a check-in: either the full link the QR code
 * encodes (which carries its own session - the only option at a bare
 * /attend), or just the token when the page was opened on the session's own
 * link. A path-only link (/attend/…?token=…) is accepted too.
 *
 * @returns {{sessionId: string, token: string}|null}
 */
function parsePasted(text, sessionIdFromPath) {
  const trimmed = text.trim()
  if (!trimmed) {
    return null
  }

  const fromLink =
    parseCheckInUrl(trimmed) ?? (trimmed.startsWith('/') ? parseCheckInUrl(`${window.location.origin}${trimmed}`) : null)
  if (fromLink) {
    return fromLink
  }

  // Anything else is taken as a bare token - unless it plainly is a link
  // (just not a valid check-in one), which no session would accept.
  return sessionIdFromPath && !trimmed.includes('/') ? { sessionId: sessionIdFromPath, token: trimmed } : null
}

/** "Marked present for Java Fundamentals — Mon, 2 Mar 2026", degrading to
 * whatever of the course and date the response carries. */
function checkedInSummary(record) {
  const parts = [record.courseName, record.sessionDate && formatSessionDate(record.sessionDate)].filter(Boolean)
  return parts.length > 0 ? `Marked present for ${parts.join(' — ')}` : 'Marked present'
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
  const { sessionId: sessionIdFromPath } = useParams()
  const [searchParams] = useSearchParams()
  const { user } = useAuth()
  const checkIn = useQrCheckInMutation()
  const { mutate } = checkIn
  const [manualInput, setManualInput] = useState('')

  const tokenFromUrl = searchParams.get('token')
  const isStudent = user.role === 'STUDENT'

  const submit = useCallback((scanned) => mutate(scanned), [mutate])
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
          <CardTitle>Check-in is for students</CardTitle>
          <CardDescription>
            Attendance for your own sessions is marked from the schedule, where you can also show the QR code.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild variant="outline">
            <Link to={`/${user.role.toLowerCase()}/schedule`}>Go to schedule</Link>
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
            You’re marked present
          </CardTitle>
          <CardDescription>{checkedInSummary(record)}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-2 text-sm">
          <p className="text-muted-foreground">
            Recorded at {new Date(record.markedAt).toLocaleTimeString(undefined, {
              hour: '2-digit',
              minute: '2-digit',
            })}
            . Nothing else to do — your trainer sees this straight away.
          </p>
          <Button asChild variant="outline" size="sm">
            <Link to="/student/schedule">Back to my schedule</Link>
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
          Check in
        </CardTitle>
        <CardDescription>
          {checkIn.isPending ? 'Checking you in…' : 'Scan the code your trainer has on screen.'}
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-4">
        {failure && (
          <div className="rounded-md border border-destructive/40 p-3">
            <p className="text-sm font-medium text-destructive">{failure.title}</p>
            <p className="text-sm text-muted-foreground">{failure.detail}</p>
          </div>
        )}

        {!checkIn.isPending && <QrScanner onScan={submit} />}

        <div className="space-y-2">
          <Label htmlFor="manualInput">Can’t scan? Paste the check-in link</Label>
          <div className="flex gap-2">
            <Input
              id="manualInput"
              value={manualInput}
              placeholder={sessionIdFromPath ? 'Check-in link or code' : 'https://…/attend/…?token=…'}
              onChange={(event) => setManualInput(event.target.value)}
            />
            <Button disabled={!pasted || checkIn.isPending} onClick={() => submit(pasted)}>
              Check in
            </Button>
          </div>
          {manualInput.trim() && !pasted && (
            <p className="text-xs text-muted-foreground">
              {sessionIdFromPath
                ? 'That isn’t a check-in link or code.'
                : 'Paste the whole check-in link — a code on its own doesn’t say which session it’s for.'}
            </p>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
