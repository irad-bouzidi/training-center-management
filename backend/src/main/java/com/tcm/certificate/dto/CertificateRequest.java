package com.tcm.certificate.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Asks for a certificate to be issued for one student on one course. */
public record CertificateRequest(
        @NotNull UUID studentId,
        @NotNull UUID courseId
) {
}
