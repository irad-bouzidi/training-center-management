package com.tcm.certificate;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Certificate settings, bound from {@code certificates.*} (see
 * application.yml) - constructor-injected the same way {@code JwtService}
 * reads its own.
 */
@Component
@Getter
public class CertificateProperties {

    /**
     * Directory the generated PDFs are written to. A named Docker volume is
     * mounted here, so certificates survive a container being replaced.
     */
    private final String storagePath;

    /** Percentage of marked sessions a student must have attended to be certified. */
    private final double minimumAttendanceRate;

    public CertificateProperties(@Value("${certificates.storage-path}") String storagePath,
                                  @Value("${certificates.minimum-attendance-rate}") double minimumAttendanceRate) {
        this.storagePath = storagePath;
        this.minimumAttendanceRate = minimumAttendanceRate;
    }
}
