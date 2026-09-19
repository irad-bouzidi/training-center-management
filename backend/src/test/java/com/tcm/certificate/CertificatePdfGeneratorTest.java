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
        byte[] pdf = generator.generate(certificate());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void generate_drawsTheCertificateItWasGiven() {
        // Content streams are compressed, so the drawn text can't be read
        // back as-is; what can be checked is that the document is a real
        // page's worth and that it depends on its subject.
        Certificate awarded = certificate();
        byte[] pdf = generator.generate(awarded);

        assertThat(pdf).hasSizeGreaterThan(1000);

        awarded.getStudent().setLastName("Someone-Else-Entirely");
        assertThat(generator.generate(awarded)).isNotEqualTo(pdf);
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
