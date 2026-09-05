# TCM-26 — Frontend Certificates

**Branch**: `TCM-26-frontend-certificates`
**Depends on**: TCM-15, TCM-25

## Goal

Admin/Trainer UI to generate certificates; Student UI to view/download
theirs. Enables the "Certificates" tab stub from `TCM-15`.

## Steps

1. `src/api/certificateApi.js` + hooks (`useGenerateCertificateMutation`,
   `useCertificatesQuery`, download via a direct authorized `fetch`/axios
   blob response since it's a binary file, not JSON).
2. Populate the "Certificates" tab on `StudentSummaryPage` (`TCM-15`) with
   a "Generate Certificate" button (disabled + tooltip explaining why when
   ineligible, using the API's error message) and a list of already-issued
   certificates with "Download" buttons.
3. `src/features/certificates/MyCertificatesPage.jsx`
   (`/student/certificates`) — the student's own issued certificates,
   "Download PDF" action (triggers a blob download via an in-memory
   object URL — note: artifact/document sandboxes may differ from the real
   app; this is a normal browser app, not an artifact, so real file
   downloads work).
4. Nav: "My Certificates" (Student).

## Acceptance Criteria

- Admin/Trainer can generate a certificate from a student's summary page
  once eligible, and it appears immediately in the list.
- Student can download their own certificate as a real PDF file.

## Out of Scope

- Bulk/batch certificate generation across a whole course roster (not
  required by the brief; can be a future enhancement).
