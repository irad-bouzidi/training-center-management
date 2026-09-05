# TCM-28 — Frontend QR Code Attendance (Bonus Feature)

**Branch**: `TCM-28-frontend-qr-attendance`
**Depends on**: TCM-20, TCM-27

## Goal

Trainer-facing QR display for a session; student-facing scan/check-in
page reachable by phone.

## Steps

1. `src/api/qrAttendanceApi.js` + hooks (`useGenerateSessionQrMutation`,
   `useQrCheckinMutation`).
2. `src/features/attendance/SessionQrDialog.jsx` — on the Trainer's
   `MarkAttendancePage` (`TCM-20`) or session detail, a "Show QR" button
   opens a `Dialog` rendering the base64 PNG large enough to scan, a live
   countdown to `expiresAt`, and a "Regenerate" button.
3. Install a lightweight browser QR-scanning library (e.g.
   `html5-qrcode` or `qr-scanner`, CDN-installable via npm) for the student
   side.
4. `src/features/attendance/QrCheckinPage.jsx` (public-ish route
   `/attend/:sessionId`, still behind `ProtectedRoute` so the student must
   be logged in, but reachable directly via the QR's encoded URL without
   going through app navigation) — requests camera permission, scans a QR
   pointing at this same app's URL pattern, extracts `token` from the query
   string (or accepts manual token paste as a fallback for devices without
   camera access), calls the check-in mutation, shows a success/error
   toast and state ("You're marked present for {course} — {session
   date}").
5. Ensure the QR-encoded URL's origin (`FRONTEND_BASE_URL` used by the
   backend in `TCM-27`) matches the deployed frontend origin — surface this
   as a required env var in `.env.example`/README if not already present.

## Acceptance Criteria

- Trainer can display a session's QR code on screen.
- A student, logged in on their phone, scanning that QR (or opening the
  encoded link directly) lands on the check-in page and is marked present
  without any manual trainer action.
- Expired QR codes show a clear "expired, ask your trainer to refresh"
  state instead of a raw error.

## Out of Scope

- Offline scanning / native app (bonus feature is delivered as a web
  flow, consistent with the rest of the app).
