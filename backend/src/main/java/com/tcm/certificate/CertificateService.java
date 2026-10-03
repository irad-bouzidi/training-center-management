package com.tcm.certificate;

import com.tcm.certificate.dto.CertificateResponse;
import java.util.List;
import java.util.UUID;

public interface CertificateService {

    /**
     * Issues a certificate: checks eligibility
     * ({@link CertificateEligibilityService}), refuses a second one for the
     * same pair, draws the PDF in English and in French, stores both under
     * {@code certificates.storage-path} and records the row.
     *
     * @param requesterIsAdmin whether the caller holds ROLE_ADMIN - anyone
     *                         else must be the course's primary trainer.
     * @throws com.tcm.common.ConflictException if the pair already has one,
     *                                          naming the existing number.
     * @throws com.tcm.common.BadRequestException if the student isn't eligible,
     *                                            saying why.
     */
    CertificateResponse generate(UUID studentId, UUID courseId, UUID issuerId, boolean requesterIsAdmin);

    /**
     * The stored PDF's bytes in one language, for the student themselves,
     * the course's trainer or an admin. A certificate issued before French
     * versions existed gets both its PDFs redrawn and recorded here, the
     * first time either is asked for.
     */
    DownloadableCertificate download(UUID certificateId, CertificateLanguage language,
                                     UUID requesterId, boolean requesterIsAdmin);

    /**
     * Certificates issued to one student, for the student themselves, a
     * trainer or an admin.
     */
    List<CertificateResponse> findForStudent(UUID studentId, UUID requesterId, boolean requesterIsAdmin);

    /** Certificates issued on the courses a trainer is the primary trainer of. */
    List<CertificateResponse> findForTrainer(UUID trainerId);

    /** A stored PDF, with the filename to offer it under. */
    record DownloadableCertificate(String filename, byte[] content) {
    }
}
