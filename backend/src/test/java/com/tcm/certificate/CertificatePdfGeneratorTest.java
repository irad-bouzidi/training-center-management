package com.tcm.certificate;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcm.certificate.model.Certificate;
import com.tcm.course.model.Course;
import com.tcm.course.model.CourseStatus;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The drawing half of TCM-25, which needs nothing but a Certificate. */
class CertificatePdfGeneratorTest {

    private final CertificatePdfGenerator generator = new CertificatePdfGenerator();

    @Test
    void generate_producesANonEmptyPdf() {
        byte[] pdf = generator.generate(certificate(), CertificateLanguage.EN);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void generate_writesPdf17_whichEveryViewerOpens() {
        // OpenPDF 3 defaults to 2.0, under which the unembedded Helvetica is
        // non-conformant and strict readers refuse the file.
        for (CertificateLanguage language : CertificateLanguage.values()) {
            byte[] pdf = generator.generate(certificate(), language);
            assertThat(new String(pdf, 0, 8, StandardCharsets.US_ASCII)).isEqualTo("%PDF-1.7");
        }
    }

    @Test
    void generate_inEachLanguage_drawsADifferentDocument() {
        Certificate awarded = certificate();

        assertThat(generator.generate(awarded, CertificateLanguage.FR))
                .isNotEqualTo(generator.generate(awarded, CertificateLanguage.EN));
    }

    @Test
    void generate_drawsTheCertificateItWasGiven() {
        // Content streams are compressed, so the drawn text can't be read
        // back as-is; what can be checked is that the document is a real
        // page's worth and that it depends on its subject.
        Certificate awarded = certificate();
        byte[] pdf = generator.generate(awarded, CertificateLanguage.EN);

        assertThat(pdf).hasSizeGreaterThan(1000);

        awarded.getStudent().setLastName("Someone-Else-Entirely");
        assertThat(generator.generate(awarded, CertificateLanguage.EN)).isNotEqualTo(pdf);
    }

    private static Certificate certificate() {
        return Certificate.builder()
                .id(UUID.randomUUID())
                .student(user("Sam", "Student", Role.STUDENT))
                .course(Course.builder()
                        .id(UUID.randomUUID()).code("JAVA-101").name("Java Fundamentals")
                        .durationHours(40).capacity(20).price(BigDecimal.TEN)
                        .status(CourseStatus.PUBLISHED)
                        .build())
                .certificateNumber("CERT-2026-000001")
                .issuedAt(Instant.parse("2026-03-02T10:15:30Z"))
                .generatedBy(user("Ada", "Admin", Role.ADMIN))
                .filePath("/tmp/CERT-2026-000001.pdf")
                .build();
    }

    private static User user(String firstName, String lastName, Role role) {
        return User.builder()
                .id(UUID.randomUUID())
                .firstName(firstName)
                .lastName(lastName)
                .email(firstName.toLowerCase() + "@tcm.local")
                .passwordHash("hash")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
