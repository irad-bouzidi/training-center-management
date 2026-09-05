package com.tcm.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import com.tcm.common.BadRequestException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.course.model.Course;
import com.tcm.course.model.CourseStatus;
import com.tcm.enrollment.EnrollmentRepository;
import com.tcm.enrollment.mapper.EnrollmentMapper;
import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.user.dto.ResetPasswordResponse;
import com.tcm.user.dto.StudentDirectoryResponse;
import com.tcm.user.dto.StudentSummaryResponse;
import com.tcm.user.dto.UserRequest;
import com.tcm.user.dto.UserResponse;
import com.tcm.user.mapper.UserMapper;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit test with a mocked repository (per TCM-8) - the real {@link UserMapper}
 * is used as-is since it's pure mapping logic, not the thing under test.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository, new UserMapper(), passwordEncoder, enrollmentRepository, new EnrollmentMapper());
    }

    private static User existingUser(UUID id, Role role) {
        return User.builder()
                .id(id)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .passwordHash("old-hash")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void create_withValidRequest_hashesPasswordAndSaves() {
        UserRequest request = new UserRequest("Jane", "Doe", "jane.doe@example.com", "Secret123!", null, Role.STUDENT);
        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.create(request);

        assertThat(response.email()).isEqualTo("jane.doe@example.com");
        assertThat(response.role()).isEqualTo(Role.STUDENT);
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void create_withBlankPassword_throwsBadRequest() {
        UserRequest request = new UserRequest("Jane", "Doe", "jane.doe@example.com", " ", null, Role.STUDENT);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_withDuplicateEmail_throwsBadRequest() {
        UserRequest request = new UserRequest("Jane", "Doe", "jane.doe@example.com", "Secret123!", null, Role.STUDENT);
        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void update_nonAdminChangingRole_throwsAccessDenied() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, Role.STUDENT);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        UserRequest request = new UserRequest("Jane", "Doe", "jane.doe@example.com", null, null, Role.TRAINER);

        assertThatThrownBy(() -> userService.update(id, request, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void update_nonAdminKeepingSameRole_succeeds() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, Role.STUDENT);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UserRequest request = new UserRequest("Janet", "Doe", "jane.doe@example.com", null, null, Role.STUDENT);

        UserResponse response = userService.update(id, request, false);

        assertThat(response.firstName()).isEqualTo("Janet");
    }

    @Test
    void update_adminChangingRole_succeeds() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, Role.STUDENT);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UserRequest request = new UserRequest("Jane", "Doe", "jane.doe@example.com", null, null, Role.TRAINER);

        UserResponse response = userService.update(id, request, true);

        assertThat(response.role()).isEqualTo(Role.TRAINER);
    }

    @Test
    void update_toAnotherUsersEmail_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, Role.STUDENT);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
        UserRequest request = new UserRequest("Jane", "Doe", "taken@example.com", null, null, Role.STUDENT);

        assertThatThrownBy(() -> userService.update(id, request, true))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void changeStatus_updatesStatus() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, Role.STUDENT);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.changeStatus(id, UserStatus.INACTIVE);

        assertThat(response.status()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void findById_unknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resetPassword_hashesAndSavesNewTempPassword() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, Role.STUDENT);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(any())).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ResetPasswordResponse response = userService.resetPassword(id);

        assertThat(response.tempPassword()).hasSize(12);
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void searchStudents_alwaysFiltersByStudentRole() {
        User student = existingUser(UUID.randomUUID(), Role.STUDENT);
        Pageable pageable = Pageable.unpaged();
        when(userRepository.search(eq(Role.STUDENT), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(java.util.List.of(student)));

        Page<?> response = userService.searchStudents(null, null, pageable);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    void searchStudents_populatesActiveEnrollmentCount() {
        UUID id = UUID.randomUUID();
        User student = existingUser(id, Role.STUDENT);
        Pageable pageable = Pageable.unpaged();
        when(userRepository.search(eq(Role.STUDENT), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(student)));
        when(enrollmentRepository.countByStudentIdAndStatus(id, EnrollmentStatus.APPROVED)).thenReturn(3L);

        Page<StudentDirectoryResponse> response = userService.searchStudents(null, null, pageable);

        assertThat(response.getContent().get(0).activeEnrollments()).isEqualTo(3);
    }

    @Test
    void getStudentSummary_returnsProfileWithStubbedFields() {
        UUID id = UUID.randomUUID();
        User student = existingUser(id, Role.STUDENT);
        when(userRepository.findById(id)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentId(id)).thenReturn(List.of());

        StudentSummaryResponse response = userService.getStudentSummary(id);

        assertThat(response.profile().id()).isEqualTo(id);
        assertThat(response.enrollments()).isEmpty();
        assertThat(response.attendanceRate()).isNull();
        assertThat(response.grades()).isEmpty();
        assertThat(response.paymentBalance()).isNull();
        assertThat(response.certificates()).isEmpty();
    }

    @Test
    void getStudentSummary_populatesRealEnrollments() {
        UUID id = UUID.randomUUID();
        User student = existingUser(id, Role.STUDENT);
        when(userRepository.findById(id)).thenReturn(Optional.of(student));
        Course course = Course.builder()
                .id(UUID.randomUUID()).code("JAVA-101").name("Java Fundamentals")
                .durationHours(40).capacity(20).price(BigDecimal.TEN)
                .status(CourseStatus.PUBLISHED)
                .build();
        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID()).student(student).course(course)
                .status(EnrollmentStatus.PENDING)
                .build();
        when(enrollmentRepository.findByStudentId(id)).thenReturn(List.of(enrollment));

        StudentSummaryResponse response = userService.getStudentSummary(id);

        assertThat(response.enrollments()).hasSize(1);
        assertThat(response.enrollments().get(0).course().code()).isEqualTo("JAVA-101");
        assertThat(response.enrollments().get(0).status()).isEqualTo(EnrollmentStatus.PENDING);
    }

    @Test
    void getStudentSummary_nonStudentUser_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        User trainer = existingUser(id, Role.TRAINER);
        when(userRepository.findById(id)).thenReturn(Optional.of(trainer));

        assertThatThrownBy(() -> userService.getStudentSummary(id))
                .isInstanceOf(BadRequestException.class);
    }
}
