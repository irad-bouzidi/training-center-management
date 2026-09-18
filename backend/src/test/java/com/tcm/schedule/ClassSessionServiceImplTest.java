package com.tcm.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.tcm.common.BadRequestException;
import com.tcm.common.ConflictException;
import com.tcm.course.CourseRepository;
import com.tcm.course.model.Course;
import com.tcm.course.model.CourseStatus;
import com.tcm.schedule.dto.ClassSessionRequest;
import com.tcm.schedule.dto.ClassSessionResponse;
import com.tcm.schedule.mapper.ClassSessionMapper;
import com.tcm.schedule.model.ClassSession;
import com.tcm.schedule.model.SessionStatus;
import com.tcm.user.UserRepository;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit test with mocked repositories - the real {@link ClassSessionMapper} is
 * used as-is since it's pure mapping logic, not the thing under test. The
 * overlap query itself is the repository's job; what's tested here is how its
 * result is turned into the right rejection.
 */
@ExtendWith(MockitoExtension.class)
class ClassSessionServiceImplTest {

    private static final LocalDate DATE = LocalDate.of(2026, 3, 2);
    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final LocalTime ELEVEN = LocalTime.of(11, 0);

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    private ClassSessionServiceImpl classSessionService;

    private UUID courseId;
    private UUID trainerId;

    @BeforeEach
    void setUp() {
        classSessionService = new ClassSessionServiceImpl(
                classSessionRepository, courseRepository, userRepository, new ClassSessionMapper());
        courseId = UUID.randomUUID();
        trainerId = UUID.randomUUID();
    }

    private ClassSessionRequest request(String classroom) {
        return new ClassSessionRequest(courseId, trainerId, classroom, DATE, NINE, ELEVEN);
    }

    private static Course course(UUID id) {
        return Course.builder()
                .id(id).code("JAVA-101").name("Java Fundamentals")
                .durationHours(40).capacity(20).price(BigDecimal.valueOf(500))
                .status(CourseStatus.PUBLISHED)
                .build();
    }

    private static User user(UUID id, Role role) {
        return User.builder()
                .id(id).firstName("Tina").lastName("Trainer")
                .email("tina-" + id + "@example.com").passwordHash("hash")
                .role(role).status(UserStatus.ACTIVE)
                .build();
    }

    private static ClassSession session(UUID id, UUID trainerId, String classroom, SessionStatus status) {
        return ClassSession.builder()
                .id(id)
                .course(course(UUID.randomUUID()))
                .trainer(user(trainerId, Role.TRAINER))
                .classroom(classroom)
                .sessionDate(DATE).startTime(NINE).endTime(ELEVEN)
                .status(status)
                .build();
    }

    /** Lets a test stand up the happy path without restating it every time. */
    private void givenCourseAndTrainerExist() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId)));
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(user(trainerId, Role.TRAINER)));
    }

    @Test
    void create_withNoClash_savesScheduledSession() {
        givenCourseAndTrainerExist();
        when(classSessionRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(classSessionRepository.save(any(ClassSession.class))).thenAnswer(inv -> inv.getArgument(0));

        ClassSessionResponse response = classSessionService.create(request("Room A"));

        assertThat(response.status()).isEqualTo(SessionStatus.SCHEDULED);
        assertThat(response.classroom()).isEqualTo("Room A");
        assertThat(response.trainer().id()).isEqualTo(trainerId);
    }

    @Test
    void create_whenTrainerIsDoubleBooked_throwsConflict() {
        givenCourseAndTrainerExist();
        when(classSessionRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(session(UUID.randomUUID(), trainerId, "Room B", SessionStatus.SCHEDULED)));

        assertThatThrownBy(() -> classSessionService.create(request("Room A")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("trainer");
    }

    @Test
    void create_whenClassroomIsDoubleBooked_throwsConflict() {
        givenCourseAndTrainerExist();
        when(classSessionRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(session(UUID.randomUUID(), UUID.randomUUID(), "Room A", SessionStatus.SCHEDULED)));

        assertThatThrownBy(() -> classSessionService.create(request("Room A")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("classroom");
    }

    /** The clashing row is the same room under different capitalisation - the
     * repository matches case-insensitively, and so must the message logic. */
    @Test
    void create_whenClassroomClashesOnlyByCase_stillThrowsConflict() {
        givenCourseAndTrainerExist();
        when(classSessionRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(session(UUID.randomUUID(), UUID.randomUUID(), "room a", SessionStatus.SCHEDULED)));

        assertThatThrownBy(() -> classSessionService.create(request("Room A")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("classroom");
    }

    @Test
    void create_whenBothTrainerAndClassroomClash_saysSo() {
        givenCourseAndTrainerExist();
        when(classSessionRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(session(UUID.randomUUID(), trainerId, "Room A", SessionStatus.SCHEDULED)));

        assertThatThrownBy(() -> classSessionService.create(request("Room A")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("both");
    }

    @Test
    void create_withEndTimeNotAfterStartTime_throwsBadRequest() {
        ClassSessionRequest request = new ClassSessionRequest(courseId, trainerId, "Room A", DATE, ELEVEN, ELEVEN);

        assertThatThrownBy(() -> classSessionService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("endTime");
    }

    @Test
    void create_withNonTrainerUser_throwsBadRequest() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId)));
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(user(trainerId, Role.STUDENT)));

        assertThatThrownBy(() -> classSessionService.create(request("Room A")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("TRAINER");
    }

    /** Rescheduling a session mustn't report it as clashing with itself. */
    @Test
    void update_doesNotTreatTheSessionItselfAsAClash() {
        UUID id = UUID.randomUUID();
        givenCourseAndTrainerExist();
        when(classSessionRepository.findById(id))
                .thenReturn(Optional.of(session(id, trainerId, "Room A", SessionStatus.SCHEDULED)));
        when(classSessionRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(session(id, trainerId, "Room A", SessionStatus.SCHEDULED)));
        when(classSessionRepository.save(any(ClassSession.class))).thenAnswer(inv -> inv.getArgument(0));

        ClassSessionResponse response = classSessionService.update(id, request("Room A"));

        assertThat(response.classroom()).isEqualTo("Room A");
    }

    @Test
    void cancel_alreadyCompletedSession_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        when(classSessionRepository.findById(id))
                .thenReturn(Optional.of(session(id, trainerId, "Room A", SessionStatus.COMPLETED)));

        assertThatThrownBy(() -> classSessionService.cancel(id))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void markCompleted_byAssignedTrainer_succeeds() {
        UUID id = UUID.randomUUID();
        when(classSessionRepository.findById(id))
                .thenReturn(Optional.of(session(id, trainerId, "Room A", SessionStatus.SCHEDULED)));
        when(classSessionRepository.save(any(ClassSession.class))).thenAnswer(inv -> inv.getArgument(0));

        ClassSessionResponse response = classSessionService.markCompleted(id, trainerId, false);

        assertThat(response.status()).isEqualTo(SessionStatus.COMPLETED);
    }

    @Test
    void markCompleted_byAnotherTrainer_throwsAccessDenied() {
        UUID id = UUID.randomUUID();
        when(classSessionRepository.findById(id))
                .thenReturn(Optional.of(session(id, trainerId, "Room A", SessionStatus.SCHEDULED)));

        assertThatThrownBy(() -> classSessionService.markCompleted(id, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void markCompleted_byAdminWhoIsNotTheTrainer_succeeds() {
        UUID id = UUID.randomUUID();
        when(classSessionRepository.findById(id))
                .thenReturn(Optional.of(session(id, trainerId, "Room A", SessionStatus.SCHEDULED)));
        when(classSessionRepository.save(any(ClassSession.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(classSessionService.markCompleted(id, UUID.randomUUID(), true).status())
                .isEqualTo(SessionStatus.COMPLETED);
    }

    /** CANCELLED sessions free their room and trainer, so the overlap query is
     * asked to ignore them rather than the service filtering them afterwards. */
    @Test
    void create_asksTheRepositoryToIgnoreCancelledSessions() {
        givenCourseAndTrainerExist();
        when(classSessionRepository.findOverlapping(eq(DATE), eq(NINE), eq(ELEVEN), eq(trainerId), eq("Room A"),
                eq(SessionStatus.CANCELLED))).thenReturn(List.of());
        when(classSessionRepository.save(any(ClassSession.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(classSessionService.create(request("Room A")).status()).isEqualTo(SessionStatus.SCHEDULED);
    }
}
