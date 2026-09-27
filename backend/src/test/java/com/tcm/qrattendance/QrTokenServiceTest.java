package com.tcm.qrattendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcm.common.BadRequestException;
import com.tcm.common.GoneException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.course.model.Course;
import com.tcm.schedule.ClassSessionRepository;
import com.tcm.schedule.model.ClassSession;
import com.tcm.schedule.model.SessionStatus;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test with a mocked repository holding one in-memory session, so a
 * token issued by the service is exactly what it later validates against.
 */
@ExtendWith(MockitoExtension.class)
class QrTokenServiceTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID TRAINER_ID = UUID.randomUUID();
    private static final long VALIDITY_MINUTES = 5;

    @Mock
    private ClassSessionRepository classSessionRepository;

    private QrTokenService qrTokenService;

    private ClassSession session;

    @BeforeEach
    void setUp() {
        qrTokenService = new QrTokenService(classSessionRepository);
        ReflectionTestUtils.setField(qrTokenService, "secret", "unit-test-qr-secret-that-is-long-enough");
        ReflectionTestUtils.setField(qrTokenService, "validityMinutes", VALIDITY_MINUTES);
        session = session(SESSION_ID, SessionStatus.SCHEDULED);
    }

    @Test
    void issue_byTheAssignedTrainer_storesTheTokenAndExpiry_andItValidates() {
        givenSession();

        QrTokenService.IssuedToken issued = qrTokenService.issue(SESSION_ID, TRAINER_ID, false);

        assertThat(issued.token()).contains(".");
        assertThat(issued.expiresAt())
                .isCloseTo(Instant.now().plus(VALIDITY_MINUTES, ChronoUnit.MINUTES), within(10, ChronoUnit.SECONDS));
        assertThat(session.getQrToken()).isEqualTo(issued.token());
        assertThat(session.getQrExpiresAt()).isEqualTo(issued.expiresAt());
        verify(classSessionRepository).save(session);

        assertThat(qrTokenService.requireValid(SESSION_ID, issued.token())).isSameAs(session);
    }

    @Test
    void issue_byAnAdmin_isAllowedForAnySession() {
        givenSession();

        assertThat(qrTokenService.issue(SESSION_ID, UUID.randomUUID(), true).token()).isNotBlank();
    }

    @Test
    void issue_byATrainerNotAssignedToTheSession_isDenied() {
        givenSession();

        assertThatThrownBy(() -> qrTokenService.issue(SESSION_ID, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
        verify(classSessionRepository, never()).save(any());
    }

    @Test
    void issue_forACancelledSession_isRejected() {
        session.setStatus(SessionStatus.CANCELLED);
        givenSession();

        assertThatThrownBy(() -> qrTokenService.issue(SESSION_ID, TRAINER_ID, false))
                .isInstanceOf(BadRequestException.class);
        verify(classSessionRepository, never()).save(any());
    }

    @Test
    void issue_forAnUnknownSession_isNotFound() {
        when(classSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrTokenService.issue(SESSION_ID, TRAINER_ID, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requireValid_afterExpiry_isGone() {
        givenSession();
        String token = qrTokenService.issue(SESSION_ID, TRAINER_ID, false).token();
        session.setQrExpiresAt(Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> qrTokenService.requireValid(SESSION_ID, token))
                .isInstanceOf(GoneException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void requireValid_withATokenThatHasBeenReplaced_isGone() {
        givenSession();
        String first = qrTokenService.issue(SESSION_ID, TRAINER_ID, false).token();
        String second = qrTokenService.issue(SESSION_ID, TRAINER_ID, false).token();

        assertThat(second).isNotEqualTo(first);
        assertThatThrownBy(() -> qrTokenService.requireValid(SESSION_ID, first))
                .isInstanceOf(GoneException.class)
                .hasMessageContaining("replaced");
        assertThat(qrTokenService.requireValid(SESSION_ID, second)).isSameAs(session);
    }

    @Test
    void requireValid_withATamperedSignature_isRejected() {
        givenSession();
        String token = qrTokenService.issue(SESSION_ID, TRAINER_ID, false).token();
        String random = token.substring(0, token.indexOf('.'));

        assertThatThrownBy(() -> qrTokenService.requireValid(SESSION_ID, random + ".forged-signature"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requireValid_withATamperedRandomHalf_isRejected() {
        givenSession();
        String token = qrTokenService.issue(SESSION_ID, TRAINER_ID, false).token();
        String signature = token.substring(token.indexOf('.') + 1);

        assertThatThrownBy(() -> qrTokenService.requireValid(SESSION_ID, "someone-elses-random." + signature))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requireValid_withAMalformedOrMissingToken_isRejected() {
        givenSession();

        assertThatThrownBy(() -> qrTokenService.requireValid(SESSION_ID, "no-dot-at-all"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> qrTokenService.requireValid(SESSION_ID, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requireValid_withAnotherSessionsToken_isRejected() {
        UUID otherSessionId = UUID.randomUUID();
        ClassSession other = session(otherSessionId, SessionStatus.SCHEDULED);
        when(classSessionRepository.findById(otherSessionId)).thenReturn(Optional.of(other));
        givenSession();
        String otherToken = qrTokenService.issue(otherSessionId, TRAINER_ID, false).token();
        qrTokenService.issue(SESSION_ID, TRAINER_ID, false);

        // The signature binds the session id, so it fails before any lookup of the stored token.
        assertThatThrownBy(() -> qrTokenService.requireValid(SESSION_ID, otherToken))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requireValid_forASessionCancelledAfterTheCodeWasShown_isRejected() {
        givenSession();
        String token = qrTokenService.issue(SESSION_ID, TRAINER_ID, false).token();
        session.setStatus(SessionStatus.CANCELLED);

        assertThatThrownBy(() -> qrTokenService.requireValid(SESSION_ID, token))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cancelled");
    }

    private void givenSession() {
        when(classSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
    }

    private static ClassSession session(UUID id, SessionStatus status) {
        User trainer = User.builder().id(TRAINER_ID).firstName("Tina").lastName("Trainer")
                .email("tina@example.com").role(Role.TRAINER).build();
        Course course = Course.builder().id(UUID.randomUUID()).code("JAVA-101").name("Java").build();
        return ClassSession.builder()
                .id(id)
                .course(course)
                .trainer(trainer)
                .classroom("Room A")
                .sessionDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .status(status)
                .build();
    }
}
