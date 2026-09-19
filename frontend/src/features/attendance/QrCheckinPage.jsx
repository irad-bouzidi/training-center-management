import { CheckCircle2, QrCode } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAuth } from '@/context/AuthContext'
import { QrScanner } from './QrScanner'
import { checkInFailure, useQrCheckInMutation } from './qrHooks'

/**
 * Where a scanned QR code lands (/attend/:sessionId?token=…), and where a
 * student can scan one from inside the app (/attend) - see
 * docs/tasks/TCM-28-frontend-qr-attendance.md step 4.
 *
 * Arriving with a token in the URL checks in straight away: the student has
 * already acted by pointing their camera at the code, and asking them to
 * press another button would be asking twice. Without one, the camera opens,
 * with a paste box for devices that won't give it up.
 */
export function QrCheckinPage() {
  const { sessionId: sessionIdFromPath } = useParams()
  const [searchParams] = useSearchParams()
  const { user } = useAuth()
  const checkIn = useQrCheckInMutation()
  const { mutate } = checkIn
  const [manualToken, setManualToken] = useState('')

  const tokenFromUrl = searchParams.get('token')
  const isStudent = user.role === 'STUDENT'

  const submit = useCallback((scanned) => mutate(scanned), [mutate])

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
          <CardDescription>{record.student.name}</CardDescription>
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
          <Label htmlFor="manualToken">Can’t scan? Paste the code</Label>
          <div className="flex gap-2">
            <Input
              id="manualToken"
              value={manualToken}
              placeholder="Code from the screen"
              onChange={(event) => setManualToken(event.target.value)}
            />
            <Button
              disabled={!sessionIdFromPath || !manualToken.trim() || checkIn.isPending}
              onClick={() => submit({ sessionId: sessionIdFromPath, token: manualToken.trim() })}
            >
              Check in
            </Button>
          </div>
          {!sessionIdFromPath && (
            <p className="text-xs text-muted-foreground">
              Pasting a code works once you’ve opened the session’s own link — scanning carries the session with it.
            </p>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
