package com.tcm.qrattendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.tcm.attendance.AttendanceService;
import com.tcm.attendance.dto.AttendanceResponse;
import com.tcm.attendance.model.AttendanceStatus;
import com.tcm.common.BadRequestException;
import com.tcm.common.GoneException;
import com.tcm.course.model.Course;
import com.tcm.enrollment.EnrollmentRepository;
import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.qrattendance.dto.QrCodeResponse;
import com.tcm.schedule.model.ClassSession;
import com.tcm.schedule.model.SessionStatus;
import com.tcm.shortlink.ShortLinkService;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test with the token service, image renderer, attendance service and
 * enrollment repository mocked. What this service owns is who may be marked
 * and that the mark goes through the QR path ({@code markViaQr}, which
 * records {@code method=QR}); the token rules have their own test in
 * {@link QrTokenServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class QrAttendanceServiceTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final UUID TRAINER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final String TOKEN = "random+/half.signature";
    private static final String FRONTEND = "http://localhost:5173";

    @Mock
    private QrTokenService qrTokenService;

    @Mock
    private QrCodeImageService qrCodeImageService;

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ShortLinkService shortLinkService;

    private QrAttendanceService qrAttendanceService;

    @BeforeEach
    void setUp() {
        qrAttendanceService = new QrAttendanceService(
                qrTokenService, qrCodeImageService, attendanceService, enrollmentRepository, shortLinkService);
        ReflectionTestUtils.setField(qrAttendanceService, "frontendBaseUrl", FRONTEND);
    }

    @Test
    void issue_returnsTheTokenAndAShortLinkToTheCheckInUrlRenderedAsAQrImage() {
        Instant expiresAt = Instant.now().plusSeconds(300);
        when(qrTokenService.issue(SESSION_ID, TRAINER_ID, false))
                .thenReturn(new QrTokenService.IssuedToken(TOKEN, expiresAt));
        byte[] png = {1, 2, 3};
        when(qrCodeImageService.render(anyString())).thenReturn(png);
        String expectedUrl = FRONTEND + "/attend/" + SESSION_ID + "?token="
                + URLEncoder.encode(TOKEN, StandardCharsets.UTF_8);
        String shortUrl = FRONTEND + "/s/abc2345";
        when(shortLinkService.shorten(expectedUrl, expiresAt)).thenReturn(shortUrl);

        QrCodeResponse response = qrAttendanceService.issue(SESSION_ID, TRAINER_ID, false);

        assertThat(response.token()).isEqualTo(TOKEN);
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
        assertThat(response.checkInUrl()).isEqualTo(expectedUrl);
        assertThat(response.shortUrl()).isEqualTo(shortUrl);
        assertThat(response.imageBase64()).isEqualTo(Base64.getEncoder().encodeToString(png));
        // The image carries the short link, which expires with the token.
        verify(qrCodeImageService).render(shortUrl);
    }

    @Test
    void checkIn_byAnApprovedStudent_marksThemPresentThroughTheQrPath() {
        givenValidToken();
        givenApprovedRoster(STUDENT_ID);
        AttendanceResponse marked = mock(AttendanceResponse.class);
        when(attendanceService.markViaQr(SESSION_ID, STUDENT_ID, AttendanceStatus.PRESENT)).thenReturn(marked);

        assertThat(qrAttendanceService.checkIn(SESSION_ID, TOKEN, STUDENT_ID)).isSameAs(marked);
        verify(attendanceService).markViaQr(SESSION_ID, STUDENT_ID, AttendanceStatus.PRESENT);
        verifyNoMoreInteractions(attendanceService);
    }

    @Test
    void checkIn_twice_delegatesToTheSameUpsertBothTimes() {
        givenValidToken();
        givenApprovedRoster(STUDENT_ID);
        AttendanceResponse marked = mock(AttendanceResponse.class);
        when(attendanceService.markViaQr(SESSION_ID, STUDENT_ID, AttendanceStatus.PRESENT)).thenReturn(marked);

        AttendanceResponse first = qrAttendanceService.checkIn(SESSION_ID, TOKEN, STUDENT_ID);
        AttendanceResponse second = qrAttendanceService.checkIn(SESSION_ID, TOKEN, STUDENT_ID);

        // Idempotency itself (one record per session/student) lives in
        // AttendanceServiceImpl#markViaQr; here the second scan must be the
        // same call, not a different kind of mark.
        assertThat(second).isSameAs(first);
        verify(attendanceService, times(2)).markViaQr(SESSION_ID, STUDENT_ID, AttendanceStatus.PRESENT);
    }

    @Test
    void checkIn_byAStudentWithoutAnApprovedEnrollment_isDenied() {
        givenValidToken();
        givenApprovedRoster(UUID.randomUUID());

        assertThatThrownBy(() -> qrAttendanceService.checkIn(SESSION_ID, TOKEN, STUDENT_ID))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(attendanceService);
    }

    @Test
    void checkIn_onlyConsultsTheApprovedRoster() {
        givenValidToken();
        when(enrollmentRepository.findByCourseIdAndStatus(COURSE_ID, EnrollmentStatus.APPROVED))
                .thenReturn(List.of());

        assertThatThrownBy(() -> qrAttendanceService.checkIn(SESSION_ID, TOKEN, STUDENT_ID))
                .isInstanceOf(AccessDeniedException.class);
        verify(enrollmentRepository).findByCourseIdAndStatus(COURSE_ID, EnrollmentStatus.APPROVED);
    }

    @Test
    void checkIn_forACancelledSession_isRejectedBeforeAnyMarking() {
        when(qrTokenService.requireValid(SESSION_ID, TOKEN))
                .thenThrow(new BadRequestException("This session was cancelled, so there is nothing to check in to"));

        assertThatThrownBy(() -> qrAttendanceService.checkIn(SESSION_ID, TOKEN, STUDENT_ID))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(enrollmentRepository, attendanceService);
    }

    @Test
    void checkIn_withAnExpiredCode_isGone() {
        when(qrTokenService.requireValid(SESSION_ID, TOKEN)).thenThrow(new GoneException("expired"));

        assertThatThrownBy(() -> qrAttendanceService.checkIn(SESSION_ID, TOKEN, STUDENT_ID))
                .isInstanceOf(GoneException.class);
        verifyNoInteractions(enrollmentRepository, attendanceService);
    }

    private void givenValidToken() {
        Course course = Course.builder().id(COURSE_ID).code("JAVA-101").name("Java").build();
        ClassSession session = ClassSession.builder()
                .id(SESSION_ID)
                .course(course)
                .trainer(user(TRAINER_ID, Role.TRAINER))
                .status(SessionStatus.SCHEDULED)
                .build();
        when(qrTokenService.requireValid(SESSION_ID, TOKEN)).thenReturn(session);
    }

    private void givenApprovedRoster(UUID... studentIds) {
        List<Enrollment> roster = java.util.Arrays.stream(studentIds)
                .map(id -> Enrollment.builder()
                        .id(UUID.randomUUID())
                        .student(user(id, Role.STUDENT))
                        .status(EnrollmentStatus.APPROVED)
                        .build())
                .toList();
        when(enrollmentRepository.findByCourseIdAndStatus(COURSE_ID, EnrollmentStatus.APPROVED)).thenReturn(roster);
    }

    private static User user(UUID id, Role role) {
        return User.builder().id(id).firstName("F").lastName("L").email(id + "@example.com").role(role).build();
    }
}
