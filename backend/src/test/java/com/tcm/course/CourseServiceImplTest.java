package com.tcm.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tcm.common.BadRequestException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.course.dto.CourseRequest;
import com.tcm.course.dto.CourseResponse;
import com.tcm.course.mapper.CourseMapper;
import com.tcm.course.model.Course;
import com.tcm.course.model.CourseStatus;
import com.tcm.enrollment.EnrollmentRepository;
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

/**
 * Unit test with mocked repositories - the real {@link CourseMapper} is used
 * as-is since it's pure mapping logic, not the thing under test.
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private CourseServiceImpl courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseServiceImpl(courseRepository, userRepository, new CourseMapper(), enrollmentRepository);
    }

    private static CourseRequest requestWithTrainer(UUID trainerId) {
        return new CourseRequest(
                "JAVA-101", "Java Fundamentals", "Intro to Java", 40, 20,
                "Programming", trainerId, BigDecimal.valueOf(500), CourseStatus.DRAFT);
    }

    private static User trainer(UUID id) {
        return User.builder()
                .id(id).firstName("Tina").lastName("Trainer")
                .email("tina@example.com").passwordHash("hash")
                .role(Role.TRAINER).status(UserStatus.ACTIVE)
                .build();
    }

    private static Course existingCourse(UUID id) {
        return Course.builder()
                .id(id).code("JAVA-101").name("Java Fundamentals")
                .durationHours(40).capacity(20).price(BigDecimal.valueOf(500))
                .status(CourseStatus.DRAFT)
                .build();
    }

    @Test
    void create_withValidTrainer_savesCourse() {
        UUID trainerId = UUID.randomUUID();
        CourseRequest request = requestWithTrainer(trainerId);
        when(courseRepository.existsByCode("JAVA-101")).thenReturn(false);
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer(trainerId)));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseResponse response = courseService.create(request);

        assertThat(response.code()).isEqualTo("JAVA-101");
        assertThat(response.primaryTrainer().id()).isEqualTo(trainerId);
        assertThat(response.status()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    void create_withNoTrainer_savesCourseWithoutTrainer() {
        CourseRequest request = requestWithTrainer(null);
        when(courseRepository.existsByCode("JAVA-101")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseResponse response = courseService.create(request);

        assertThat(response.primaryTrainer()).isNull();
    }

    @Test
    void create_withNonTrainerUser_throwsBadRequest() {
        UUID nonTrainerId = UUID.randomUUID();
        CourseRequest request = requestWithTrainer(nonTrainerId);
        when(courseRepository.existsByCode("JAVA-101")).thenReturn(false);
        User student = User.builder()
                .id(nonTrainerId).firstName("Sam").lastName("Student")
                .email("sam@example.com").passwordHash("hash")
                .role(Role.STUDENT).status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findById(nonTrainerId)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> courseService.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_withUnknownTrainerId_throwsBadRequest() {
        UUID unknownId = UUID.randomUUID();
        CourseRequest request = requestWithTrainer(unknownId);
        when(courseRepository.existsByCode("JAVA-101")).thenReturn(false);
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_withDuplicateCode_throwsBadRequest() {
        CourseRequest request = requestWithTrainer(null);
        when(courseRepository.existsByCode("JAVA-101")).thenReturn(true);

        assertThatThrownBy(() -> courseService.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void update_unknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.update(id, requestWithTrainer(null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changeStatus_updatesStatus() {
        UUID id = UUID.randomUUID();
        Course course = existingCourse(id);
        when(courseRepository.findById(id)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseResponse response = courseService.changeStatus(id, CourseStatus.PUBLISHED);

        assertThat(response.status()).isEqualTo(CourseStatus.PUBLISHED);
    }

    @Test
    void findById_unknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesCourse() {
        UUID id = UUID.randomUUID();
        Course course = existingCourse(id);
        when(courseRepository.findById(id)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByCourseId(id)).thenReturn(false);

        courseService.delete(id);

        org.mockito.Mockito.verify(courseRepository).delete(course);
    }

    @Test
    void delete_courseWithEnrollments_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        Course course = existingCourse(id);
        when(courseRepository.findById(id)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByCourseId(id)).thenReturn(true);

        assertThatThrownBy(() -> courseService.delete(id))
                .isInstanceOf(BadRequestException.class);

        org.mockito.Mockito.verify(courseRepository, org.mockito.Mockito.never()).delete(any(Course.class));
    }
}
