import { Html5Qrcode } from 'html5-qrcode'
import { useEffect, useId, useState } from 'react'

const SCAN_CONFIG = { fps: 10, qrbox: { width: 240, height: 240 } }

/**
 * Reads a check-in URL out of the phone's camera. Pulls the session and
 * token straight out of the scanned URL, which is the same
 * `/attend/{sessionId}?token=…` the backend encodes (QrAttendanceService),
 * so scanning inside the app and opening the link from the camera app end up
 * in exactly the same place.
 *
 * @returns {{sessionId: string, token: string}|null} null for a QR that
 *          isn't one of ours - a poster, a payment code, anything.
 */
export function parseCheckInUrl(text) {
  try {
    const url = new URL(text)
    const match = /\/attend\/([0-9a-fA-F-]{36})$/.exec(url.pathname)
    const token = url.searchParams.get('token')
    return match && token ? { sessionId: match[1], token } : null
  } catch {
    return null
  }
}

/**
 * The camera half of the student check-in page. The scanner is an external
 * system with its own lifecycle - it's started when this mounts and stopped
 * when it unmounts, which is exactly what an effect is for.
 */
export function QrScanner({ onScan }) {
  const elementId = useId().replace(/:/g, '')
  const [error, setError] = useState(null)

  useEffect(() => {
    const scanner = new Html5Qrcode(elementId)
    let started = false

    scanner
      .start({ facingMode: 'environment' }, SCAN_CONFIG, (text) => {
        const scanned = parseCheckInUrl(text)
        if (scanned) {
          onScan(scanned)
        }
      })
      // Decode failures fire constantly while the camera hunts for a code;
      // only a failure to start the camera at all is worth telling anyone.
      .then(() => {
        started = true
      })
      .catch(() =>
        setError('This device’s camera isn’t available. Open the link from your camera app, or paste the code below.'),
      )

    return () => {
      if (started) {
        scanner.stop().catch(() => {
          // Already stopped, or the page is going away - nothing to do.
        })
      }
    }
  }, [elementId, onScan])

  return (
    <div className="space-y-2">
      <div id={elementId} className="overflow-hidden rounded-md border" />
      {error && <p className="text-sm text-muted-foreground">{error}</p>}
    </div>
  )
}
