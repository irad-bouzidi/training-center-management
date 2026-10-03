package com.tcm.certificate;

import com.tcm.certificate.dto.CertificateResponse;
import com.tcm.certificate.mapper.CertificateMapper;
import com.tcm.certificate.model.Certificate;
import com.tcm.common.BadRequestException;
import com.tcm.common.ConflictException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.course.CourseRepository;
import com.tcm.course.model.Course;
import com.tcm.user.UserRepository;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CertificateEligibilityService eligibilityService;
    private final CertificatePdfGenerator pdfGenerator;
    private final CertificateMapper certificateMapper;
    private final CertificateProperties properties;

    @Override
    @Transactional
    public CertificateResponse generate(UUID studentId, UUID courseId, UUID issuerId, boolean requesterIsAdmin) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("No course with id " + courseId));
        requireTeaches(course, issuerId, requesterIsAdmin);

        certificateRepository.findByStudentIdAndCourseId(studentId, courseId).ifPresent(existing -> {
            // The id is in the message so the caller can go straight to the
            // certificate that already exists instead of searching for it.
            throw new ConflictException("Certificate " + existing.getCertificateNumber()
                    + " (id " + existing.getId() + ") already issued to this student for this course");
        });

        String reason = eligibilityService.ineligibilityReason(studentId, courseId);
        if (reason != null) {
            throw new BadRequestException(reason);
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new BadRequestException("No user with id " + studentId));
        User issuer = userRepository.findById(issuerId)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id " + issuerId));

        Instant issuedAt = Instant.now();
        Certificate certificate = Certificate.builder()
                .student(student)
                .course(course)
                .certificateNumber(nextCertificateNumber(issuedAt))
                .issuedAt(issuedAt)
                .generatedBy(issuer)
                .build();

        // The row carries where the PDFs went, so the files are written
        // first and their paths recorded - a row pointing at nothing would
        // be worse than a file nothing points at.
        certificate.setFilePath(store(certificate, CertificateLanguage.EN).toString());
        certificate.setFilePathFr(store(certificate, CertificateLanguage.FR).toString());
        return certificateMapper.toResponse(certificateRepository.save(certificate));
    }

    @Override
    @Transactional
    public DownloadableCertificate download(UUID certificateId, CertificateLanguage language,
                                            UUID requesterId, boolean requesterIsAdmin) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("No certificate with id " + certificateId));
        requireMayRead(certificate, requesterId, requesterIsAdmin);

        if (certificate.getFilePathFr() == null) {
            redrawLegacy(certificate);
        }

        String filePath = language == CertificateLanguage.FR ? certificate.getFilePathFr() : certificate.getFilePath();

        try {
            return new DownloadableCertificate(
                    certificate.getCertificateNumber() + "-" + language.code() + ".pdf",
                    Files.readAllBytes(Path.of(filePath)));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Certificate " + certificate.getCertificateNumber() + " is recorded but its PDF is missing", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificateResponse> findForStudent(UUID studentId, UUID requesterId, boolean requesterIsAdmin) {
        if (!requesterIsAdmin && !studentId.equals(requesterId) && !isTrainer(requesterId)) {
            throw new AccessDeniedException("You may only read your own certificates");
        }
        return certificateRepository.findByStudentIdOrderByIssuedAtDesc(studentId).stream()
                .map(certificateMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificateResponse> findForTrainer(UUID trainerId) {
        return certificateRepository.findByCoursePrimaryTrainerIdOrderByIssuedAtDesc(trainerId).stream()
                .map(certificateMapper::toResponse)
                .toList();
    }

    /**
     * A certificate issued before French versions existed has only an
     * English PDF, written as PDF 2.0 - the file some readers refused to
     * open. Both languages are drawn afresh from the same record (same
     * number, same issue date), recorded, and the old file removed.
     */
    private void redrawLegacy(Certificate certificate) {
        Path previous = Path.of(certificate.getFilePath());
        Path english = store(certificate, CertificateLanguage.EN);
        certificate.setFilePath(english.toString());
        certificate.setFilePathFr(store(certificate, CertificateLanguage.FR).toString());
        certificateRepository.save(certificate);

        if (!previous.equals(english)) {
            try {
                Files.deleteIfExists(previous);
            } catch (IOException e) {
                // An orphaned old PDF is harmless; nothing points at it now.
            }
        }
    }

    /** {@code CERT-<year>-<6 digits>}, the sequence coming from the database. */
    private String nextCertificateNumber(Instant issuedAt) {
        long sequence = certificateRepository.nextCertificateSequence();
        return "CERT-%d-%06d".formatted(issuedAt.atZone(ZoneId.systemDefault()).getYear(), sequence);
    }

    /**
     * Writes one language's PDF under {@code certificates.storage-path} as
     * {@code <number>-<lang>.pdf} and returns where it landed.
     */
    private Path store(Certificate certificate, CertificateLanguage language) {
        try {
            Path directory = Path.of(properties.getStoragePath());
            Files.createDirectories(directory);
            Path file = directory.resolve(certificate.getCertificateNumber() + "-" + language.code() + ".pdf");
            Files.write(file, pdfGenerator.generate(certificate, language));
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store the certificate PDF", e);
        }
    }

    /** The course's primary trainer, or an admin. */
    private static void requireTeaches(Course course, UUID requesterId, boolean requesterIsAdmin) {
        if (requesterIsAdmin) {
            return;
        }
        User trainer = course.getPrimaryTrainer();
        if (trainer == null || !trainer.getId().equals(requesterId)) {
            throw new AccessDeniedException("You may only certify students on courses you teach");
        }
    }

    /** The student it belongs to, the course's trainer, or an admin. */
    private static void requireMayRead(Certificate certificate, UUID requesterId, boolean requesterIsAdmin) {
        if (requesterIsAdmin || certificate.getStudent().getId().equals(requesterId)) {
            return;
        }
        User trainer = certificate.getCourse().getPrimaryTrainer();
        if (trainer == null || !trainer.getId().equals(requesterId)) {
            throw new AccessDeniedException("You may only download your own certificates");
        }
    }

    private boolean isTrainer(UUID requesterId) {
        return userRepository.findById(requesterId)
                .map(user -> user.getRole() == Role.TRAINER)
                .orElse(false);
    }
}
