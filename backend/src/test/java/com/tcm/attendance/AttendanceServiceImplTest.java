package com.tcm.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcm.attendance.dto.AttendanceMarkRequest;
import com.tcm.attendance.dto.CourseAttendanceReportResponse;
import com.tcm.attendance.dto.SessionRosterResponse;
import com.tcm.attendance.mapper.AttendanceMapper;
import com.tcm.attendance.model.AttendanceMethod;
import com.tcm.attendance.model.AttendanceRecord;
import com.tcm.attendance.model.AttendanceStatus;
import com.tcm.common.BadRequestException;
import com.tcm.course.CourseRepository;
import com.tcm.course.model.Course;
import com.tcm.course.model.CourseStatus;
import com.tcm.enrollment.EnrollmentRepository;
import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.schedule.ClassSessionRepository;
import com.tcm.schedule.mapper.ClassSessionMapper;
import com.tcm.schedule.model.ClassSession;
import com.tcm.schedule.model.SessionStatus;
import com.tcm.user.UserRepository;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
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
 * Unit test with mocked repositories (per TCM-19). The real mappers are used
 * as-is - they're pure mapping logic, not the thing under test.
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final UUID TRAINER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    private AttendanceServiceImpl attendanceService;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceServiceImpl(
                attendanceRepository, classSessionRepository, enrollmentRepository, courseRepository,
                userRepository, new AttendanceMapper(), new ClassSessionMapper());
    }

    @Test
    void getRoster_listsApprovedStudents_withNullStatusWhenUnmarked() {
        givenSession();
        givenApprovedRoster(student(STUDENT_ID, "Sam", "Student"));
        when(attendanceRepository.findBySessionId(SESSION_ID)).thenReturn(List.of());

        SessionRosterResponse roster = attendanceService.getRoster(SESSION_ID, TRAINER_ID, false);

        assertThat(roster.session().id()).isEqualTo(SESSION_ID);
        assertThat(roster.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.studentId()).isEqualTo(STUDENT_ID);
            assertThat(entry.status()).isNull();
            assertThat(entry.markedAt()).isNull();
        });
    }

    @Test
    void getRoster_byATrainerWhoIsNotAssigned_isDenied() {
        givenSession();

        assertThatThrownBy(() -> attendanceService.getRoster(SESSION_ID, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getRoster_byAnAdmin_isAllowedForAnySession() {
        givenSession();
        givenApprovedRoster(student(STUDENT_ID, "Sam", "Student"));
        when(attendanceRepository.findBySessionId(SESSION_ID)).thenReturn(List.of());

        assertThat(attendanceService.getRoster(SESSION_ID, UUID.randomUUID(), true).entries()).hasSize(1);
    }

    @Test
    void markBulk_updatesTheExistingRecord_ratherThanAddingASecond() {
        ClassSession session = givenSession();
        User student = student(STUDENT_ID, "Sam", "Student");
        givenApprovedRoster(student);
        AttendanceRecord existing = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .session(session)
                .student(student)
                .status(AttendanceStatus.ABSENT)
                .method(AttendanceMethod.MANUAL)
                .markedAt(Instant.EPOCH)
                .build();
        when(attendanceRepository.findBySessionIdAndStudentId(SESSION_ID, STUDENT_ID))
                .thenReturn(Optional.of(existing));
        when(userRepository.getReferenceById(TRAINER_ID)).thenReturn(trainer());
        when(attendanceRepository.save(any(AttendanceRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        var saved = attendanceService.markBulk(
                SESSION_ID, List.of(new AttendanceMarkRequest(STUDENT_ID, AttendanceStatus.PRESENT)),
                TRAINER_ID, false);

        assertThat(saved).singleElement().satisfies(response -> {
            assertThat(response.id()).isEqualTo(existing.getId());
            assertThat(response.status()).isEqualTo(AttendanceStatus.PRESENT);
            assertThat(response.method()).isEqualTo(AttendanceMethod.MANUAL);
        });
        assertThat(existing.getMarkedAt()).isAfter(Instant.EPOCH);
    }

    @Test
    void markBulk_withTheStatusAlreadyRecorded_leavesAQrCheckInUntouched() {
        ClassSession session = givenSession();
        User student = student(STUDENT_ID, "Sam", "Student");
        givenApprovedRoster(student);
        Instant checkedInAt = Instant.parse("2026-03-02T09:05:00Z");
        AttendanceRecord qrCheckIn = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .session(session)
                .student(student)
                .status(AttendanceStatus.PRESENT)
                .method(AttendanceMethod.QR)
                .markedAt(checkedInAt)
                .build();
        when(attendanceRepository.findBySessionIdAndStudentId(SESSION_ID, STUDENT_ID))
                .thenReturn(Optional.of(qrCheckIn));

        // The trainer re-saves the whole roster with this student still PRESENT.
        var saved = attendanceService.markBulk(
                SESSION_ID, List.of(new AttendanceMarkRequest(STUDENT_ID, AttendanceStatus.PRESENT)),
                TRAINER_ID, false);

        assertThat(saved).singleElement().satisfies(response -> {
            assertThat(response.method()).isEqualTo(AttendanceMethod.QR);
            assertThat(response.markedAt()).isEqualTo(checkedInAt);
        });
        assertThat(qrCheckIn.getMarkedBy()).isNull();
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void markBulk_onACancelledSession_isRejectedAndWritesNothing() {
        givenSession().setStatus(SessionStatus.CANCELLED);

        assertThatThrownBy(() -> attendanceService.markBulk(
                SESSION_ID, List.of(new AttendanceMarkRequest(STUDENT_ID, AttendanceStatus.PRESENT)),
                TRAINER_ID, false))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cancelled");
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void markOne_onACancelledSession_isRejected() {
        givenSession().setStatus(SessionStatus.CANCELLED);

        assertThatThrownBy(() -> attendanceService.markOne(
                SESSION_ID, STUDENT_ID, AttendanceStatus.PRESENT, TRAINER_ID, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cancelled");
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void markBulk_byATrainerWhoIsNotAssigned_isDeniedAndWritesNothing() {
        givenSession();

        assertThatThrownBy(() -> attendanceService.markBulk(
                SESSION_ID, List.of(new AttendanceMarkRequest(STUDENT_ID, AttendanceStatus.PRESENT)),
                UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void markOne_forAStudentWithoutAnApprovedEnrollment_isRejected() {
        givenSession();
        givenApprovedRoster();

        assertThatThrownBy(() -> attendanceService.markOne(
                SESSION_ID, STUDENT_ID, AttendanceStatus.PRESENT, TRAINER_ID, false))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("APPROVED or COMPLETED enrollment");
    }

    @Test
    void courseAttendanceReport_aggregatesPerStudent_andCountsLateAsAttended() {
        UUID otherStudentId = UUID.randomUUID();
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
        givenApprovedRoster(student(STUDENT_ID, "Ada", "Attendee"), student(otherStudentId, "Zoe", "Zero"));
        when(classSessionRepository.countByCourseId(COURSE_ID)).thenReturn(4L);
        when(attendanceRepository.countByCourseGroupedByStudentAndStatus(COURSE_ID)).thenReturn(List.of(
                statusCount(STUDENT_ID, AttendanceStatus.PRESENT, 2),
                statusCount(STUDENT_ID, AttendanceStatus.LATE, 1),
                statusCount(STUDENT_ID, AttendanceStatus.ABSENT, 1)));

        CourseAttendanceReportResponse report = attendanceService.courseAttendanceReport(COURSE_ID, null, true);

        assertThat(report.sessionCount()).isEqualTo(4);
        assertThat(report.students()).hasSize(2);
        CourseAttendanceReportResponse.StudentRow marked = report.students().get(0);
        assertThat(marked.studentId()).isEqualTo(STUDENT_ID);
        assertThat(marked.present()).isEqualTo(2);
        assertThat(marked.late()).isEqualTo(1);
        assertThat(marked.absent()).isEqualTo(1);
        assertThat(marked.marked()).isEqualTo(4);
        assertThat(marked.attendanceRate()).isEqualTo(75.0);

        // Nobody has marked this one, so they're unrated rather than 0%.
        CourseAttendanceReportResponse.StudentRow unmarked = report.students().get(1);
        assertThat(unmarked.marked()).isZero();
        assertThat(unmarked.attendanceRate()).isNull();
    }

    @Test
    void courseAttendanceReport_keepsStudentsWhoseEnrollmentIsCompleted() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
        when(enrollmentRepository.findByCourseIdAndStatusIn(COURSE_ID, ATTENDING))
                .thenReturn(List.of(enrollment(student(STUDENT_ID, "Cleo", "Completed"), EnrollmentStatus.COMPLETED)));
        when(attendanceRepository.countByCourseGroupedByStudentAndStatus(COURSE_ID)).thenReturn(List.of(
                statusCount(STUDENT_ID, AttendanceStatus.PRESENT, 3)));

        CourseAttendanceReportResponse report = attendanceService.courseAttendanceReport(COURSE_ID, null, true);

        assertThat(report.students()).singleElement().satisfies(row -> {
            assertThat(row.studentId()).isEqualTo(STUDENT_ID);
            assertThat(row.present()).isEqualTo(3);
            assertThat(row.attendanceRate()).isEqualTo(100.0);
        });
    }

    @Test
    void findMine_mapsEachRecordWithItsSessionAndCourse_inTheRepositorysOrder() {
        ClassSession later = session(UUID.randomUUID(), LocalDate.of(2026, 3, 9));
        ClassSession earlier = session(SESSION_ID, LocalDate.of(2026, 3, 2));
        User student = student(STUDENT_ID, "Sam", "Student");
        when(attendanceRepository.findByStudentIdNewestFirst(STUDENT_ID)).thenReturn(List.of(
                record(later, student, AttendanceStatus.LATE, AttendanceMethod.MANUAL),
                record(earlier, student, AttendanceStatus.PRESENT, AttendanceMethod.QR)));

        var mine = attendanceService.findMine(STUDENT_ID);

        assertThat(mine).hasSize(2);
        assertThat(mine.get(0).sessionId()).isEqualTo(later.getId());
        assertThat(mine.get(0).sessionDate()).isEqualTo(LocalDate.of(2026, 3, 9));
        assertThat(mine.get(0).status()).isEqualTo(AttendanceStatus.LATE);
        assertThat(mine.get(1).sessionId()).isEqualTo(SESSION_ID);
        assertThat(mine.get(1).method()).isEqualTo(AttendanceMethod.QR);
        assertThat(mine.get(1).courseId()).isEqualTo(COURSE_ID);
        assertThat(mine.get(1).courseCode()).isEqualTo("JAVA-101");
        assertThat(mine.get(1).courseName()).isEqualTo("Java Fundamentals");
        assertThat(mine.get(1).startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(mine.get(1).endTime()).isEqualTo(LocalTime.of(11, 0));
    }

    @Test
    void courseAttendanceReport_byATrainerWhoDoesNotTeachTheCourse_isDenied() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
        UUID stranger = UUID.randomUUID();
        when(classSessionRepository.existsByCourseIdAndTrainerId(COURSE_ID, stranger)).thenReturn(false);

        assertThatThrownBy(() -> attendanceService.courseAttendanceReport(COURSE_ID, stranger, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void courseAttendanceReport_byTheCoursesSessionTrainer_isAllowed() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
        when(classSessionRepository.existsByCourseIdAndTrainerId(COURSE_ID, TRAINER_ID)).thenReturn(true);
        givenApprovedRoster();
        when(attendanceRepository.countByCourseGroupedByStudentAndStatus(COURSE_ID)).thenReturn(List.of());

        assertThat(attendanceService.courseAttendanceReport(COURSE_ID, TRAINER_ID, false).students()).isEmpty();
    }

    @Test
    void studentCourseAttendanceRate_countsOnlyThatCourse() {
        when(attendanceRepository.countByStudentAndCourseGroupedByStatus(STUDENT_ID, COURSE_ID)).thenReturn(List.of(
                statusTotal(AttendanceStatus.PRESENT, 3),
                statusTotal(AttendanceStatus.ABSENT, 1)));

        assertThat(attendanceService.studentCourseAttendanceRate(STUDENT_ID, COURSE_ID)).isEqualTo(75.0);
    }

    @Test
    void studentCourseAttendanceRate_isNullWhenNothingIsMarkedOnIt() {
        when(attendanceRepository.countByStudentAndCourseGroupedByStatus(STUDENT_ID, COURSE_ID))
                .thenReturn(List.of());

        assertThat(attendanceService.studentCourseAttendanceRate(STUDENT_ID, COURSE_ID)).isNull();
    }

    @Test
    void studentAttendanceSummary_isNullUntilSomethingIsMarked() {
        when(attendanceRepository.countByStudentGroupedByStatus(STUDENT_ID)).thenReturn(List.of());

        assertThat(attendanceService.studentAttendanceSummary(STUDENT_ID)).isNull();
    }

    @Test
    void studentAttendanceSummary_isThePercentageOfMarkedSessionsAttended() {
        when(attendanceRepository.countByStudentGroupedByStatus(STUDENT_ID)).thenReturn(List.of(
                statusTotal(AttendanceStatus.PRESENT, 1),
                statusTotal(AttendanceStatus.LATE, 1),
                statusTotal(AttendanceStatus.ABSENT, 1)));

        assertThat(attendanceService.studentAttendanceSummary(STUDENT_ID)).isEqualTo(66.7);
    }

    private ClassSession givenSession() {
        ClassSession session = session(SESSION_ID, LocalDate.of(2026, 3, 2));
        when(classSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        return session;
    }

    private static ClassSession session(UUID id, LocalDate date) {
        return ClassSession.builder()
                .id(id)
                .course(course())
                .trainer(trainer())
                .classroom("Room A")
                .sessionDate(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .status(SessionStatus.SCHEDULED)
                .build();
    }

    private static AttendanceRecord record(ClassSession session, User student, AttendanceStatus status,
                                           AttendanceMethod method) {
        return AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .session(session)
                .student(student)
                .status(status)
                .method(method)
                .markedAt(Instant.EPOCH)
                .build();
    }

    /** The enrollment statuses whose students the roster and report list. */
    private static final List<EnrollmentStatus> ATTENDING =
            List.of(EnrollmentStatus.APPROVED, EnrollmentStatus.COMPLETED);

    private void givenApprovedRoster(User... students) {
        when(enrollmentRepository.findByCourseIdAndStatusIn(COURSE_ID, ATTENDING))
                .thenReturn(List.of(students).stream()
                        .map(student -> enrollment(student, EnrollmentStatus.APPROVED))
                        .toList());
    }

    private static Enrollment enrollment(User student, EnrollmentStatus status) {
        return Enrollment.builder()
                .id(UUID.randomUUID())
                .student(student)
                .course(course())
                .status(status)
                .enrolledAt(Instant.EPOCH)
                .build();
    }

    private static Course course() {
        return Course.builder()
                .id(COURSE_ID).code("JAVA-101").name("Java Fundamentals")
                .durationHours(40).capacity(20).price(BigDecimal.TEN)
                .status(CourseStatus.PUBLISHED)
                .build();
    }

    private static User trainer() {
        return User.builder()
                .id(TRAINER_ID)
                .firstName("Tom")
                .lastName("Trainer")
                .email("tom@tcm.local")
                .passwordHash("hash")
                .role(Role.TRAINER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private static User student(UUID id, String firstName, String lastName) {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .email(firstName.toLowerCase() + "@tcm.local")
                .passwordHash("hash")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private static AttendanceRepository.StatusCount statusCount(UUID studentId, AttendanceStatus status, long total) {
        return new AttendanceRepository.StatusCount() {
            @Override
            public UUID getStudentId() {
                return studentId;
            }

            @Override
            public AttendanceStatus getStatus() {
                return status;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private static AttendanceRepository.StatusTotal statusTotal(AttendanceStatus status, long total) {
        return new AttendanceRepository.StatusTotal() {
            @Override
            public AttendanceStatus getStatus() {
                return status;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }
}
