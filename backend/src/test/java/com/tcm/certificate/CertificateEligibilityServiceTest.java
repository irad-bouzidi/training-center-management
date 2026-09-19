package com.tcm.certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcm.attendance.AttendanceService;
import com.tcm.course.model.Course;
import com.tcm.course.model.CourseStatus;
import com.tcm.enrollment.EnrollmentRepository;
import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The one rule TCM-25 certifies on, checked in each of its states. */
@ExtendWith(MockitoExtension.class)
class CertificateEligibilityServiceTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private AttendanceService attendanceService;

    private CertificateEligibilityService eligibilityService;

    @BeforeEach
    void setUp() {
        eligibilityService = new CertificateEligibilityService(
                enrollmentRepository, attendanceService, new CertificateProperties("/tmp/tcm-certificates", 75));
    }

    @Test
    void aCompletedEnrollmentWithEnoughAttendance_isEligible() {
        givenEnrollment(EnrollmentStatus.COMPLETED);
        givenAttendanceRate(80.0);

        assertThat(eligibilityService.ineligibilityReason(STUDENT_ID, COURSE_ID)).isNull();
    }

    @Test
    void attendanceExactlyAtTheThreshold_isEligible() {
        givenEnrollment(EnrollmentStatus.COMPLETED);
        givenAttendanceRate(75.0);

        assertThat(eligibilityService.ineligibilityReason(STUDENT_ID, COURSE_ID)).isNull();
    }

    @Test
    void anEnrollmentThatIsNotCompleted_isNotEligible() {
        givenEnrollment(EnrollmentStatus.APPROVED);

        assertThat(eligibilityService.ineligibilityReason(STUDENT_ID, COURSE_ID))
                .contains("COMPLETED")
                .contains("APPROVED");
    }

    @Test
    void attendanceBelowTheThreshold_isNotEligible() {
        givenEnrollment(EnrollmentStatus.COMPLETED);
        givenAttendanceRate(60.0);

        assertThat(eligibilityService.ineligibilityReason(STUDENT_ID, COURSE_ID))
                .contains("60.0%")
                .contains("75.0%");
    }

    @Test
    void anUnmarkedStudent_isNotEligible_ratherThanBeingReadAsZero() {
        givenEnrollment(EnrollmentStatus.COMPLETED);
        givenAttendanceRate(null);

        assertThat(eligibilityService.ineligibilityReason(STUDENT_ID, COURSE_ID))
                .contains("No attendance has been recorded");
    }

    @Test
    void aStudentWhoIsNotEnrolled_isNotEligible() {
        when(enrollmentRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of());

        assertThat(eligibilityService.ineligibilityReason(STUDENT_ID, COURSE_ID)).contains("not enrolled");
    }

    private void givenEnrollment(EnrollmentStatus status) {
        when(enrollmentRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(Enrollment.builder()
                .id(UUID.randomUUID())
                .student(student())
                .course(course())
                .status(status)
                .enrolledAt(Instant.EPOCH)
                .build()));
    }

    private void givenAttendanceRate(Double rate) {
        when(attendanceService.studentCourseAttendanceRate(STUDENT_ID, COURSE_ID)).thenReturn(rate);
    }

    private static Course course() {
        return Course.builder()
                .id(COURSE_ID).code("JAVA-101").name("Java Fundamentals")
                .durationHours(40).capacity(20).price(BigDecimal.TEN)
                .status(CourseStatus.PUBLISHED)
                .build();
    }

    private static User student() {
        return User.builder()
                .id(STUDENT_ID)
                .firstName("Sam")
                .lastName("Student")
                .email("sam@tcm.local")
                .passwordHash("hash")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
