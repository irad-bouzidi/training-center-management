import { RefreshCw } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useGenerateSessionQrMutation } from './qrHooks'

/** "4:37" left, or "expired" once the clock runs out. */
function remaining(expiresAt, now) {
  const seconds = Math.floor((new Date(expiresAt).getTime() - now) / 1000)
  if (seconds <= 0) {
    return null
  }
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`
}

/**
 * The code a trainer puts on screen for a session (TCM-27/28). Opening the
 * dialog mints one, and "New code" mints another - each replaces the last,
 * so only what's on screen right now works.
 *
 * Mounted only while open (see MarkAttendancePage), so it never holds a code
 * nobody is looking at.
 */
export function SessionQrDialog({ onOpenChange, session }) {
  const generate = useGenerateSessionQrMutation(session.id)
  const { mutate } = generate
  const code = generate.data
  const [now, setNow] = useState(() => Date.now())

  // One code on opening.
  useEffect(() => {
    mutate()
  }, [mutate])

  // A ticking countdown, so the room can see when it's about to go stale.
  useEffect(() => {
    const interval = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(interval)
  }, [])

  const countdown = code ? remaining(code.expiresAt, now) : null

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Scan to check in</DialogTitle>
          <DialogDescription>
            {session.course.name} · {session.classroom}
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col items-center gap-3">
          {generate.isPending && <p className="text-sm text-muted-foreground">Producing a code…</p>}

          {code && (
            <>
              <img
                src={`data:image/png;base64,${code.imageBase64}`}
                alt={`QR code for ${session.course.name}`}
                className="size-64 rounded-md border bg-white p-2"
              />
              <p className="text-sm text-muted-foreground">
                {countdown ? (
                  <>
                    Expires in <span className="font-medium tabular-nums text-foreground">{countdown}</span>
                  </>
                ) : (
                  'This code has expired — show a new one.'
                )}
              </p>
              <p className="max-w-full truncate text-xs text-muted-foreground" title={code.checkInUrl}>
                {code.checkInUrl}
              </p>
            </>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Close
          </Button>
          <Button disabled={generate.isPending} onClick={() => mutate()}>
            <RefreshCw />
            New code
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
