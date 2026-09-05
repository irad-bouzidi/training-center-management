# TCM-27 — QR Code Attendance API (Bonus Feature)

**Branch**: `TCM-27-qr-attendance-api`
**Depends on**: TCM-19, TCM-17

## Goal

Generate a per-session QR code students scan to self-register attendance,
per the brief's bonus feature. Uses the `qr_token`/`qr_expires_at` columns
already present on `class_sessions` since `TCM-17`.

## Steps

1. Add `com.google.zxing:core` and `com.google.zxing:javase` to
   `backend/pom.xml`.
2. In `com.tcm.schedule` (or a new `com.tcm.qrattendance` package if
   preferred for separation — recommend a dedicated
   `com.tcm.qrattendance` package to keep the bonus feature cleanly
   isolated and easy to omit if ever needed):
   - `QrTokenService.generate(sessionId)` — creates a signed, short-lived
     opaque token (random UUID + HMAC signature using `JWT_SECRET` or a
     dedicated `QR_SECRET`), stores it + an expiry (e.g. now + session
     duration, or a rolling 5-minute window refreshed by the trainer) on
     the `class_sessions` row via `ClassSessionRepository`.
   - `QrCodeImageService.render(token)` — ZXing `QRCodeWriter` encodes a
     URL like `{FRONTEND_BASE_URL}/attend/{sessionId}?token={token}` into a
     PNG byte array.
   - `QrAttendanceController.java`:
     - `POST /api/v1/sessions/{sessionId}/qr` (assigned TRAINER, ADMIN) —
       (re)generates the token/expiry, returns
       `{ token, expiresAt, imageBase64 }` (or streams the PNG directly at
       a follow-up `GET` — recommend returning base64 in JSON so the
       frontend can also show the expiry countdown without a second call).
     - `POST /api/v1/attendance/qr-checkin` (STUDENT) — body
       `{ sessionId, token }`; validates token matches + not expired,
       validates the caller has an `APPROVED` enrollment in the session's
       course, then calls the existing `AttendanceService.markOne(...,
       method=QR)` from `TCM-19` — reuses that logic rather than
       duplicating it.
3. Validation/edge cases: expired token → 410 Gone with a clear message;
   token mismatch → 400; already marked → idempotent success (no duplicate
   row, per the existing unique constraint/upsert semantics from
   `TCM-19`); student not enrolled → 403.
4. Tests: token expiry enforcement, one-token-per-session-at-a-time
   (regenerating invalidates the previous token), successful check-in path,
   duplicate check-in is idempotent.

## Acceptance Criteria

- Trainer can generate a fresh QR code for a session from its detail view.
- A student hitting the check-in endpoint with a valid, unexpired token and
  an approved enrollment gets marked present via `method=QR`.
- Expired or invalid tokens are rejected with clear error responses.

## Out of Scope

- Native mobile camera scanning (handled by the browser's camera via a
  JS QR-scanning library in `TCM-28`, not backend concern).
- Frontend (`TCM-28`).
