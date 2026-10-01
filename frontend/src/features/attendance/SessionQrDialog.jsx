import { RefreshCw } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Trans, useTranslation } from 'react-i18next'
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
  const { t } = useTranslation('attendance')
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
          <DialogTitle>{t('qrDialog.title')}</DialogTitle>
          <DialogDescription>
            {session.course.name} · {session.classroom}
          </DialogDescription>
        </DialogHeader>

        {/* min-w-0 is what keeps the link from pushing the dialog open: an
            unbroken string is a single long word, and without it the flex
            column grows to fit rather than letting it wrap. */}
        <div className="flex min-w-0 flex-col items-center gap-3">
          {generate.isPending && <p className="text-sm text-muted-foreground">{t('qrDialog.producing')}</p>}

          {code && (
            <>
              <img
                src={`data:image/png;base64,${code.imageBase64}`}
                alt={t('qrDialog.imageAlt', { course: session.course.name })}
                className="size-64 rounded-md border bg-white p-2"
              />
              <p className="text-sm text-muted-foreground">
                {countdown ? (
                  <Trans
                    t={t}
                    i18nKey="qrDialog.expiresIn"
                    values={{ countdown }}
                    components={{ countdown: <span className="font-medium tabular-nums text-foreground" /> }}
                  />
                ) : (
                  t('qrDialog.expired')
                )}
              </p>
              {/* The short link, in full - what the image encodes, and short
                  enough to read off the screen and type in. */}
              <p className="w-full break-all text-center font-mono text-sm text-foreground select-all">
                {code.shortUrl}
              </p>
            </>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {t('common:actions.close')}
          </Button>
          <Button disabled={generate.isPending} onClick={() => mutate()}>
            <RefreshCw />
            {t('qrDialog.newCode')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
