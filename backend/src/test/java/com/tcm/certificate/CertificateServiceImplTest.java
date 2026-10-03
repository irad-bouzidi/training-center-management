package com.tcm.certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcm.certificate.CertificateService.DownloadableCertificate;
import com.tcm.certificate.dto.CertificateResponse;
import com.tcm.certificate.mapper.CertificateMapper;
import com.tcm.certificate.model.Certificate;
import com.tcm.common.BadRequestException;
import com.tcm.common.ConflictException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.course.CourseRepository;
import com.tcm.course.model.Course;
import com.tcm.course.model.CourseStatus;
import com.tcm.user.UserRepository;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit test with mocked repositories, eligibility check and PDF generator.
 * The real {@link CertificateMapper} is used, and the storage directory is a
 * JUnit temp dir so the "file written, path recorded" step is real.
 */
@ExtendWith(MockitoExtension.class)
class CertificateServiceImplTest {

    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final UUID TRAINER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID CERTIFICATE_ID = UUID.randomUUID();
    private static final byte[] PDF = "%PDF-fake".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PDF_FR = "%PDF-fake-fr".getBytes(StandardCharsets.UTF_8);

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CertificateEligibilityService eligibilityService;

    @Mock
    private CertificatePdfGenerator pdfGenerator;

    @TempDir
    Path storage;

    private CertificateServiceImpl certificateService;

    @BeforeEach
    void setUp() {
        certificateService = new CertificateServiceImpl(
                certificateRepository, courseRepository, userRepository, eligibilityService, pdfGenerator,
                new CertificateMapper(), new CertificateProperties(storage.toString(), 75));
    }

    // --- generate -----------------------------------------------------------

    @Test
    void generate_forAnEligibleStudent_storesThePdfAndRecordsTheNumberedCertificate() throws Exception {
        givenCourse();
        when(certificateRepository.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.empty());
        when(eligibilityService.ineligibilityReason(STUDENT_ID, COURSE_ID)).thenReturn(null);
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
        when(userRepository.findById(TRAINER_ID)).thenReturn(Optional.of(trainer()));
        when(certificateRepository.nextCertificateSequence()).thenReturn(42L);
        when(pdfGenerator.generate(any(Certificate.class), any(CertificateLanguage.class))).thenReturn(PDF);
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(inv -> inv.getArgument(0));

        CertificateResponse response = certificateService.generate(STUDENT_ID, COURSE_ID, TRAINER_ID, false);

        String expectedNumber = "CERT-%d-000042".formatted(Year.now().getValue());
        assertThat(response.certificateNumber()).isEqualTo(expectedNumber);
        assertThat(response.student().id()).isEqualTo(STUDENT_ID);
        assertThat(response.course().id()).isEqualTo(COURSE_ID);
        assertThat(response.issuedAt()).isNotNull();

        ArgumentCaptor<Certificate> saved = ArgumentCaptor.forClass(Certificate.class);
        verify(certificateRepository).save(saved.capture());
        Certificate certificate = saved.getValue();
        assertThat(certificate.getGeneratedBy().getId()).isEqualTo(TRAINER_ID);
        Path english = storage.resolve(expectedNumber + "-en.pdf");
        Path french = storage.resolve(expectedNumber + "-fr.pdf");
        assertThat(certificate.getFilePath()).isEqualTo(english.toString());
        assertThat(certificate.getFilePathFr()).isEqualTo(french.toString());
        assertThat(Files.readAllBytes(english)).isEqualTo(PDF);
        assertThat(Files.readAllBytes(french)).isEqualTo(PDF);
        verify(pdfGenerator).generate(certificate, CertificateLanguage.EN);
        verify(pdfGenerator).generate(certificate, CertificateLanguage.FR);
    }

    @Test
    void generate_byAnAdmin_isAllowedOnACourseWithNoTrainer() {
        Course course = course(null);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(certificateRepository.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.empty());
        when(eligibilityService.ineligibilityReason(STUDENT_ID, COURSE_ID)).thenReturn(null);
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin()));
        when(certificateRepository.nextCertificateSequence()).thenReturn(1L);
        when(pdfGenerator.generate(any(Certificate.class), any(CertificateLanguage.class))).thenReturn(PDF);
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(inv -> inv.getArgument(0));

        CertificateResponse response = certificateService.generate(STUDENT_ID, COURSE_ID, ADMIN_ID, true);

        assertThat(response.certificateNumber()).endsWith("-000001");
    }

    @Test
    void generate_whenThePairAlreadyHasACertificate_conflicts_andWritesNothing() {
        givenCourse();
        when(certificateRepository.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.of(certificate(course(trainer()))));

        assertThatThrownBy(() -> certificateService.generate(STUDENT_ID, COURSE_ID, TRAINER_ID, false))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(eligibilityService, pdfGenerator);
        verify(certificateRepository, never()).save(any());
    }

    @Test
    void generate_forAnIneligibleStudent_isRejectedWithTheReason() {
        givenCourse();
        when(certificateRepository.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.empty());
        when(eligibilityService.ineligibilityReason(STUDENT_ID, COURSE_ID))
                .thenReturn("Attendance on this course is 50.0%, below the 75.0% required for a certificate");

        assertThatThrownBy(() -> certificateService.generate(STUDENT_ID, COURSE_ID, TRAINER_ID, false))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("below the 75.0% required");
        verifyNoInteractions(pdfGenerator);
        verify(certificateRepository, never()).save(any());
    }

    @Test
    void generate_byATrainerWhoDoesNotTeachTheCourse_isDenied() {
        givenCourse();

        assertThatThrownBy(() -> certificateService.generate(STUDENT_ID, COURSE_ID, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(eligibilityService, pdfGenerator);
    }

    @Test
    void generate_byANonAdminOnACourseWithNoTrainer_isDenied() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course(null)));

        assertThatThrownBy(() -> certificateService.generate(STUDENT_ID, COURSE_ID, TRAINER_ID, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void generate_forAnUnknownCourse_isNotFound() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificateService.generate(STUDENT_ID, COURSE_ID, ADMIN_ID, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- download -----------------------------------------------------------

    @Test
    void download_byTheStudentItBelongsTo_returnsTheStoredBytes() throws Exception {
        givenStoredCertificate();

        DownloadableCertificate download =
                certificateService.download(CERTIFICATE_ID, CertificateLanguage.EN, STUDENT_ID, false);

        assertThat(download.filename()).isEqualTo("CERT-2026-000007-en.pdf");
        assertThat(download.content()).isEqualTo(PDF);
    }

    @Test
    void download_inFrench_returnsTheFrenchPdf() throws Exception {
        givenStoredCertificate();

        DownloadableCertificate download =
                certificateService.download(CERTIFICATE_ID, CertificateLanguage.FR, STUDENT_ID, false);

        assertThat(download.filename()).isEqualTo("CERT-2026-000007-fr.pdf");
        assertThat(download.content()).isEqualTo(PDF_FR);
        verifyNoInteractions(pdfGenerator);
    }

    @Test
    void download_ofACertificateIssuedBeforeFrenchExisted_redrawsBothLanguagesOnce() throws Exception {
        Certificate certificate = givenLegacyCertificate();
        byte[] english = "%PDF-en".getBytes(StandardCharsets.UTF_8);
        byte[] french = "%PDF-fr".getBytes(StandardCharsets.UTF_8);
        when(pdfGenerator.generate(certificate, CertificateLanguage.EN)).thenReturn(english);
        when(pdfGenerator.generate(certificate, CertificateLanguage.FR)).thenReturn(french);

        DownloadableCertificate download =
                certificateService.download(CERTIFICATE_ID, CertificateLanguage.FR, STUDENT_ID, false);

        assertThat(download.content()).isEqualTo(french);
        Path englishFile = storage.resolve("CERT-2026-000007-en.pdf");
        Path frenchFile = storage.resolve("CERT-2026-000007-fr.pdf");
        assertThat(certificate.getFilePath()).isEqualTo(englishFile.toString());
        assertThat(certificate.getFilePathFr()).isEqualTo(frenchFile.toString());
        assertThat(Files.readAllBytes(englishFile)).isEqualTo(english);
        assertThat(storage.resolve("CERT-2026-000007.pdf")).doesNotExist();
        verify(certificateRepository).save(certificate);

        // Recorded now, so the next download just reads the file.
        assertThat(certificateService.download(CERTIFICATE_ID, CertificateLanguage.EN, STUDENT_ID, false).content())
                .isEqualTo(english);
        verify(pdfGenerator).generate(certificate, CertificateLanguage.EN);
    }

    @Test
    void download_byTheCoursesTrainer_isAllowed() throws Exception {
        givenStoredCertificate();

        assertThat(certificateService.download(CERTIFICATE_ID, CertificateLanguage.EN, TRAINER_ID, false).content()).isEqualTo(PDF);
    }

    @Test
    void download_byAnAdmin_isAllowed() throws Exception {
        givenStoredCertificate();

        assertThat(certificateService.download(CERTIFICATE_ID, CertificateLanguage.EN, ADMIN_ID, true).content()).isEqualTo(PDF);
    }

    @Test
    void download_byAnotherStudent_isDenied() {
        when(certificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(certificate(course(trainer()))));

        assertThatThrownBy(() -> certificateService.download(CERTIFICATE_ID, CertificateLanguage.EN, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void download_byATrainerOfAnotherCourse_isDenied() {
        User otherTrainer = trainer();
        otherTrainer.setId(UUID.randomUUID());
        when(certificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(certificate(course(trainer()))));

        assertThatThrownBy(() -> certificateService.download(CERTIFICATE_ID, CertificateLanguage.EN, otherTrainer.getId(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void download_ofAnUnknownCertificate_isNotFound() {
        when(certificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificateService.download(CERTIFICATE_ID, CertificateLanguage.EN, ADMIN_ID, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- findForStudent -----------------------------------------------------

    @Test
    void findForStudent_byTheStudentThemselves_listsTheirCertificates() {
        when(certificateRepository.findByStudentIdOrderByIssuedAtDesc(STUDENT_ID))
                .thenReturn(List.of(certificate(course(trainer()))));

        List<CertificateResponse> result = certificateService.findForStudent(STUDENT_ID, STUDENT_ID, false);

        assertThat(result).singleElement()
                .satisfies(response -> assertThat(response.certificateNumber()).isEqualTo("CERT-2026-000007"));
    }

    @Test
    void findForStudent_byAnAdmin_isAllowed() {
        when(certificateRepository.findByStudentIdOrderByIssuedAtDesc(STUDENT_ID)).thenReturn(List.of());

        assertThat(certificateService.findForStudent(STUDENT_ID, ADMIN_ID, true)).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    void findForStudent_byATrainer_isAllowed() {
        when(userRepository.findById(TRAINER_ID)).thenReturn(Optional.of(trainer()));
        when(certificateRepository.findByStudentIdOrderByIssuedAtDesc(STUDENT_ID))
                .thenReturn(List.of(certificate(course(trainer()))));

        assertThat(certificateService.findForStudent(STUDENT_ID, TRAINER_ID, false)).hasSize(1);
    }

    @Test
    void findForStudent_byAnotherStudent_isDenied() {
        User otherStudent = student();
        otherStudent.setId(UUID.randomUUID());
        when(userRepository.findById(otherStudent.getId())).thenReturn(Optional.of(otherStudent));

        assertThatThrownBy(() -> certificateService.findForStudent(STUDENT_ID, otherStudent.getId(), false))
                .isInstanceOf(AccessDeniedException.class);
        verify(certificateRepository, never()).findByStudentIdOrderByIssuedAtDesc(any());
    }

    // --- findForTrainer -----------------------------------------------------

    @Test
    void findForTrainer_listsCertificatesOnTheirCourses() {
        when(certificateRepository.findByCoursePrimaryTrainerIdOrderByIssuedAtDesc(TRAINER_ID))
                .thenReturn(List.of(certificate(course(trainer()))));

        assertThat(certificateService.findForTrainer(TRAINER_ID)).singleElement()
                .satisfies(response -> assertThat(response.student().id()).isEqualTo(STUDENT_ID));
    }

    // --- fixtures -----------------------------------------------------------

    private void givenCourse() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course(trainer())));
    }

    /** Both languages' PDFs on disk. */
    private Certificate givenStoredCertificate() throws Exception {
        Certificate certificate = certificate(course(trainer()));
        Path english = storage.resolve(certificate.getCertificateNumber() + "-en.pdf");
        Path french = storage.resolve(certificate.getCertificateNumber() + "-fr.pdf");
        Files.write(english, PDF);
        Files.write(french, PDF_FR);
        certificate.setFilePath(english.toString());
        certificate.setFilePathFr(french.toString());
        when(certificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(certificate));
        return certificate;
    }

    /** Issued before French versions existed: one English PDF, under the old name. */
    private Certificate givenLegacyCertificate() throws Exception {
        Certificate certificate = certificate(course(trainer()));
        Path file = storage.resolve(certificate.getCertificateNumber() + ".pdf");
        Files.write(file, PDF);
        certificate.setFilePath(file.toString());
        when(certificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(certificate));
        return certificate;
    }

    private static Certificate certificate(Course course) {
        return Certificate.builder()
                .id(CERTIFICATE_ID)
                .student(student())
                .course(course)
                .certificateNumber("CERT-2026-000007")
                .issuedAt(Instant.now())
                .filePath("/nowhere/CERT-2026-000007.pdf")
                .generatedBy(course.getPrimaryTrainer() == null ? admin() : course.getPrimaryTrainer())
                .build();
    }

    private static Course course(User trainer) {
        return Course.builder()
                .id(COURSE_ID)
                .code("JAVA-101")
                .name("Java Fundamentals")
                .capacity(20)
                .durationHours(40)
                .primaryTrainer(trainer)
                .status(CourseStatus.PUBLISHED)
                .build();
    }

    private static User student() {
        return user(STUDENT_ID, "Sam", "Student", Role.STUDENT);
    }

    private static User trainer() {
        return user(TRAINER_ID, "Tina", "Trainer", Role.TRAINER);
    }

    private static User admin() {
        return user(ADMIN_ID, "Ada", "Admin", Role.ADMIN);
    }

    private static User user(UUID id, String firstName, String lastName, Role role) {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .email(firstName.toLowerCase() + "@example.com")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
