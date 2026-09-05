package com.tcm.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tcm.common.BadRequestException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.course.CourseRepository;
import com.tcm.course.model.Course;
import com.tcm.course.model.CourseStatus;
import com.tcm.enrollment.dto.EnrollmentResponse;
import com.tcm.enrollment.mapper.EnrollmentMapper;
import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.user.UserRepository;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit test with mocked repositories - the real {@link EnrollmentMapper} is
 * used as-is since it's pure mapping logic, not the thing under test.
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    private EnrollmentServiceImpl enrollmentService;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentServiceImpl(
                enrollmentRepository, userRepository, courseRepository, new EnrollmentMapper());
    }

    private static User user(UUID id, Role role) {
        return User.builder()
                .id(id).firstName("Jane").lastName("Doe")
                .email("jane-" + id + "@example.com").passwordHash("hash")
                .role(role).status(UserStatus.ACTIVE)
                .build();
    }

    private static Course course(UUID id, int capacity, CourseStatus status) {
        return Course.builder()
                .id(id).code("JAVA-101").name("Java Fundamentals")
                .durationHours(40).capacity(capacity).price(BigDecimal.valueOf(500))
                .status(status)
                .build();
    }

    private static Enrollment enrollment(UUID id, User student, Course course, EnrollmentStatus status) {
        return Enrollment.builder()
                .id(id).student(student).course(course).status(status)
                .build();
    }

    @Test
    void register_validRequest_createsPendingEnrollment() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        User student = user(studentId, Role.STUDENT);
        Course course = course(courseId, 20, CourseStatus.PUBLISHED);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(false);
        when(enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.APPROVED)).thenReturn(0L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse response = enrollmentService.register(studentId, courseId);

        assertThat(response.status()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(response.student().id()).isEqualTo(studentId);
        assertThat(response.course().id()).isEqualTo(courseId);
    }

    @Test
    void register_unpublishedCourse_throwsBadRequest() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(user(studentId, Role.STUDENT)));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId, 20, CourseStatus.DRAFT)));

        assertThatThrownBy(() -> enrollmentService.register(studentId, courseId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void register_duplicateEnrollment_throwsBadRequest() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(user(studentId, Role.STUDENT)));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId, 20, CourseStatus.PUBLISHED)));
        when(enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.register(studentId, courseId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void register_courseAtCapacity_throwsBadRequest() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(user(studentId, Role.STUDENT)));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId, 2, CourseStatus.PUBLISHED)));
        when(enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(false);
        when(enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.APPROVED)).thenReturn(2L);

        assertThatThrownBy(() -> enrollmentService.register(studentId, courseId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void register_nonStudentUser_throwsBadRequest() {
        UUID trainerId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(user(trainerId, Role.TRAINER)));

        assertThatThrownBy(() -> enrollmentService.register(trainerId, courseId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void decide_approvesPendingEnrollment() {
        UUID id = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Course course = course(UUID.randomUUID(), 20, CourseStatus.PUBLISHED);
        Enrollment enrollment = enrollment(id, user(UUID.randomUUID(), Role.STUDENT), course, EnrollmentStatus.PENDING);
        when(enrollmentRepository.findById(id)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.countByCourseIdAndStatus(course.getId(), EnrollmentStatus.APPROVED)).thenReturn(0L);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(user(adminId, Role.ADMIN)));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse response = enrollmentService.decide(id, EnrollmentStatus.APPROVED, adminId);

        assertThat(response.status()).isEqualTo(EnrollmentStatus.APPROVED);
        assertThat(response.decidedAt()).isNotNull();
        assertThat(response.decidedBy().id()).isEqualTo(adminId);
    }

    @Test
    void decide_rejectsPendingEnrollment() {
        UUID id = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Enrollment enrollment = enrollment(id, user(UUID.randomUUID(), Role.STUDENT),
                course(UUID.randomUUID(), 20, CourseStatus.PUBLISHED), EnrollmentStatus.PENDING);
        when(enrollmentRepository.findById(id)).thenReturn(Optional.of(enrollment));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(user(adminId, Role.ADMIN)));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse response = enrollmentService.decide(id, EnrollmentStatus.REJECTED, adminId);

        assertThat(response.status()).isEqualTo(EnrollmentStatus.REJECTED);
    }

    @Test
    void decide_nonPendingEnrollment_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        Enrollment enrollment = enrollment(id, user(UUID.randomUUID(), Role.STUDENT),
                course(UUID.randomUUID(), 20, CourseStatus.PUBLISHED), EnrollmentStatus.APPROVED);
        when(enrollmentRepository.findById(id)).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> enrollmentService.decide(id, EnrollmentStatus.APPROVED, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void decide_invalidDecisionValue_throwsBadRequest() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> enrollmentService.decide(id, EnrollmentStatus.CANCELLED, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void decide_approvingAtCapacity_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        Course course = course(UUID.randomUUID(), 1, CourseStatus.PUBLISHED);
        Enrollment enrollment = enrollment(id, user(UUID.randomUUID(), Role.STUDENT), course, EnrollmentStatus.PENDING);
        when(enrollmentRepository.findById(id)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.countByCourseIdAndStatus(course.getId(), EnrollmentStatus.APPROVED)).thenReturn(1L);

        assertThatThrownBy(() -> enrollmentService.decide(id, EnrollmentStatus.APPROVED, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void cancel_ownPendingEnrollment_succeeds() {
        UUID studentId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Enrollment enrollment = enrollment(id, user(studentId, Role.STUDENT),
                course(UUID.randomUUID(), 20, CourseStatus.PUBLISHED), EnrollmentStatus.PENDING);
        when(enrollmentRepository.findById(id)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse response = enrollmentService.cancel(id, studentId, false);

        assertThat(response.status()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    void cancel_someoneElsesEnrollment_throwsAccessDenied() {
        UUID id = UUID.randomUUID();
        Enrollment enrollment = enrollment(id, user(UUID.randomUUID(), Role.STUDENT),
                course(UUID.randomUUID(), 20, CourseStatus.PUBLISHED), EnrollmentStatus.PENDING);
        when(enrollmentRepository.findById(id)).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> enrollmentService.cancel(id, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cancel_asAdmin_canCancelAnyEnrollment() {
        UUID id = UUID.randomUUID();
        Enrollment enrollment = enrollment(id, user(UUID.randomUUID(), Role.STUDENT),
                course(UUID.randomUUID(), 20, CourseStatus.PUBLISHED), EnrollmentStatus.APPROVED);
        when(enrollmentRepository.findById(id)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse response = enrollmentService.cancel(id, UUID.randomUUID(), true);

        assertThat(response.status()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    void cancel_alreadyDecidedEnrollment_throwsBadRequest() {
        UUID studentId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Enrollment enrollment = enrollment(id, user(studentId, Role.STUDENT),
                course(UUID.randomUUID(), 20, CourseStatus.PUBLISHED), EnrollmentStatus.REJECTED);
        when(enrollmentRepository.findById(id)).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> enrollmentService.cancel(id, studentId, false))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void markCompleted_approvedEnrollment_succeeds() {
        UUID id = UUID.randomUUID();
        Enrollment enrollment = enrollment(id, user(UUID.randomUUID(), Role.STUDENT),
                course(UUID.randomUUID(), 20, CourseStatus.PUBLISHED), EnrollmentStatus.APPROVED);
        when(enrollmentRepository.findById(id)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse response = enrollmentService.markCompleted(id);

        assertThat(response.status()).isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    void markCompleted_nonApprovedEnrollment_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        Enrollment enrollment = enrollment(id, user(UUID.randomUUID(), Role.STUDENT),
                course(UUID.randomUUID(), 20, CourseStatus.PUBLISHED), EnrollmentStatus.PENDING);
        when(enrollmentRepository.findById(id)).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> enrollmentService.markCompleted(id))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void searchForTrainer_courseNotOwnedByTrainer_throwsAccessDenied() {
        UUID trainerId = UUID.randomUUID();
        UUID otherTrainerId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Course course = course(courseId, 20, CourseStatus.PUBLISHED);
        course.setPrimaryTrainer(user(otherTrainerId, Role.TRAINER));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> enrollmentService.searchForTrainer(
                trainerId, courseId, null, org.springframework.data.domain.Pageable.unpaged()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void searchForTrainer_unknownCourseId_throwsResourceNotFound() {
        UUID courseId = UUID.randomUUID();
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.searchForTrainer(
                UUID.randomUUID(), courseId, null, org.springframework.data.domain.Pageable.unpaged()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
