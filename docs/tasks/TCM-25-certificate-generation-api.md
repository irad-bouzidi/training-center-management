# TCM-25 — PDF Certificate Generation API

**Branch**: `TCM-25-certificate-generation-api`
**Depends on**: TCM-23, TCM-19

## Goal

Automatically generate a professional PDF certificate for a student who
completes a training program, per `docs/PLAN.md` §5 `certificates`.

## Steps

1. Add `openpdf` (or `itext7-core`, confirm license fit) dependency to
   `backend/pom.xml`.
2. Liquibase changelog: create `certificates` table (FKs to `users`,
   `courses`; unique `certificate_number`).
3. `com.tcm.certificate` package:
   - `model/Certificate.java`.
   - `CertificateRepository` — find by student, by course,
     `existsByStudentIdAndCourseId`.
   - `CertificateEligibilityService` — defines "completed": enrollment
     `status = COMPLETED` (or `APPROVED` with the course's sessions all
     `COMPLETED` — pick and document one clear rule: recommend requiring
     the enrollment to be explicitly marked `COMPLETED`, and attendance
     rate ≥ a configurable threshold, e.g. 75%, sourced from
     `application.yml`).
   - `CertificatePdfGenerator` — builds the PDF: student name, course
     name, completion date, certificate number, a simple letterhead-style
     layout (logo placeholder, title "Certificate of Completion", body
     text, signature line), returns bytes.
   - `CertificateService`/`Impl`:
     - `generate(studentId, courseId, issuerId)` — checks eligibility, checks
       not already issued, builds the PDF, stores it (local filesystem path
       under a configurable `certificates.storage-path`, mounted as a
       Docker volume — object storage is a possible future upgrade, not
       required now), inserts the `certificates` row with a generated
       `certificate_number` (e.g. `CERT-2026-000123`).
     - `download(certificateId)` — streams the stored PDF, authorized for
       the student themselves or ADMIN/assigned TRAINER.
   - `CertificateController.java`:
     - `POST /api/v1/certificates/generate` (ADMIN, assigned TRAINER) —
       body: `{ studentId, courseId }`.
     - `GET /api/v1/certificates/{id}/download` (self, ADMIN, TRAINER) —
       `Content-Type: application/pdf`.
     - `GET /api/v1/certificates?studentId=` (self, ADMIN, TRAINER).
4. Update `StudentSummaryResponse` (`TCM-13`) with the real `certificates`
   list.
5. Add `certificates.storage-path` to `application.yml`/`application-
   docker.yml`, and a named Docker volume in `docker-compose.yml` mounted
   at that path in the backend service (small addition, called out
   explicitly since it touches `TCM-5`'s file).
6. Tests: eligibility rule enforcement (rejects if not completed / below
   attendance threshold), no duplicate certificate for the same
   student+course, PDF bytes are non-empty and start with `%PDF`.

## Acceptance Criteria

- Generating a certificate for an ineligible student is rejected with a
  clear error explaining why.
- A generated certificate can be downloaded as a valid PDF by the student,
  their trainer, or an admin.
- Re-generating for an already-certified student+course pair is rejected
  (or returns the existing one — pick one and document it: recommend
  rejecting with 409, pointing at the existing certificate id).

## Out of Scope

- Frontend (`TCM-26`).
- Digital signatures / verification QR on the certificate itself (nice-to-
  have, not required by the brief).
