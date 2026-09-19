package com.tcm.certificate.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * The certificate's record. The PDF itself is fetched separately from
 * {@code GET /certificates/{id}/download}; {@code filePath} is deliberately
 * not exposed - where a file sits on the server is nobody's business but the
 * server's.
 */
public record CertificateResponse(
        UUID id,
        StudentSummary student,
        CourseSummary course,
        String certificateNumber,
        Instant issuedAt
) {
    public record StudentSummary(UUID id, String name, String email) {
    }

    public record CourseSummary(UUID id, String code, String name) {
    }
}
