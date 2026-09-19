package com.tcm.grade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcm.common.BadRequestException;
import com.tcm.course.CourseRepository;
import com.tcm.course.model.Course;
import com.tcm.course.model.CourseStatus;
import com.tcm.enrollment.EnrollmentRepository;
import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.grade.dto.GradeRequest;
import com.tcm.grade.dto.GradeResponse;
import com.tcm.grade.mapper.GradeMapper;
import com.tcm.grade.model.AssessmentType;
import com.tcm.grade.model.Grade;
import com.tcm.user.UserRepository;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
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
 * Unit test with mocked repositories (per TCM-23). The real
 * {@link GradeMapper} is used as-is since it's pure mapping logic.
 */
@ExtendWith(MockitoExtension.class)
class GradeServiceImplTest {

    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final UUID TRAINER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID GRADE_ID = UUID.randomUUID();

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private GradeServiceImpl gradeService;

    @BeforeEach
    void setUp() {
        gradeService = new GradeServiceImpl(
                gradeRepository, courseRepository, userRepository, enrollmentRepository, new GradeMapper());
    }

    @Test
    void create_byTheCoursesTrainer_recordsTheResult() {
        givenCourse();
        givenApprovedRoster(student());
        when(userRepository.findById(TRAINER_ID)).thenReturn(Optional.of(trainer()));
        when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

        GradeResponse response = gradeService.create(
                request(new BigDecimal("18.00"), new BigDecimal("20.00"), new BigDecimal("40.00")),
                TRAINER_ID, false);

        assertThat(response.score()).isEqualByComparingTo("18.00");
        assertThat(response.percentage()).isEqualTo(90.0);
        assertThat(response.gradedBy().id()).isEqualTo(TRAINER_ID);
    }

    @Test
    void create_byATrainerWhoDoesNotTeachTheCourse_isDenied() {
        givenCourse();

        assertThatThrownBy(() -> gradeService.create(
                request(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN), UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
        verify(gradeRepository, never()).save(any());
    }

    @Test
    void create_forAStudentWithoutAnApprovedEnrollment_isRejected() {
        givenCourse();
        givenApprovedRoster();

        assertThatThrownBy(() -> gradeService.create(
                request(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN), TRAINER_ID, false))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("APPROVED enrollment");
    }

    @Test
    void create_withAScoreAboveTheMaximum_isRejected() {
        givenCourse();

        assertThatThrownBy(() -> gradeService.create(
                request(new BigDecimal("21.00"), new BigDecimal("20.00"), BigDecimal.TEN), TRAINER_ID, false))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("maxScore");
    }

    @Test
    void update_byAnotherTrainer_isDenied_evenOnTheirOwnCourse() {
        when(gradeRepository.findById(GRADE_ID)).thenReturn(Optional.of(grade(
                new BigDecimal("18.00"), new BigDecimal("20.00"), new BigDecimal("40.00"))));

        assertThatThrownBy(() -> gradeService.update(
                GRADE_ID, request(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN), UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void update_byAnAdmin_isAllowed() {
        when(gradeRepository.findById(GRADE_ID)).thenReturn(Optional.of(grade(
                new BigDecimal("18.00"), new BigDecimal("20.00"), new BigDecimal("40.00"))));
        when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(gradeService.update(
                GRADE_ID, request(new BigDecimal("15.00"), new BigDecimal("20.00"), new BigDecimal("40.00")),
                UUID.randomUUID(), true).score())
                .isEqualByComparingTo("15.00");
    }

    @Test
    void weightedAverage_weighsEachAssessmentByItsShare() {
        // 90% at weight 40 and 50% at weight 60 -> (0.9*40 + 0.5*60) / 100.
        List<Grade> grades = List.of(
                grade(new BigDecimal("18.00"), new BigDecimal("20.00"), new BigDecimal("40.00")),
                grade(new BigDecimal("50.00"), new BigDecimal("100.00"), new BigDecimal("60.00")));

        assertThat(GradeServiceImpl.weightedAverage(grades)).isEqualTo(66.0);
    }

    @Test
    void weightedAverage_dividesByTheWeightsGiven_notBy100() {
        // A single 50%-weighted assessment scored 80% is an 80% average so
        // far, not 40% - a course can be graded before every assessment is set.
        List<Grade> grades = List.of(grade(new BigDecimal("80.00"), new BigDecimal("100.00"), new BigDecimal("50.00")));

        assertThat(GradeServiceImpl.weightedAverage(grades)).isEqualTo(80.0);
    }

    @Test
    void weightedAverage_isNullWithNothingGraded() {
        assertThat(GradeServiceImpl.weightedAverage(List.of())).isNull();
    }

    @Test
    void findForStudent_byTheStudentThemselves_isAllowed() {
        when(gradeRepository.findByStudentIdOrderByGradedAtDesc(STUDENT_ID)).thenReturn(List.of(
                grade(new BigDecimal("18.00"), new BigDecimal("20.00"), new BigDecimal("40.00"))));

        assertThat(gradeService.findForStudent(STUDENT_ID, null, STUDENT_ID, false).weightedAverage())
                .isEqualTo(90.0);
    }

    @Test
    void findForStudent_byATrainerWithoutNamingACourse_isDenied() {
        assertThatThrownBy(() -> gradeService.findForStudent(STUDENT_ID, null, TRAINER_ID, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findForStudent_byTheTrainerOfTheCourseAsked_isAllowed() {
        givenCourse();
        when(gradeRepository.findByStudentIdAndCourseIdOrderByGradedAtDesc(STUDENT_ID, COURSE_ID))
                .thenReturn(List.of());

        assertThat(gradeService.findForStudent(STUDENT_ID, COURSE_ID, TRAINER_ID, false).grades()).isEmpty();
    }

    @Test
    void courseGradebook_listsEveryApprovedStudent_gradedOrNot() {
        givenCourse();
        UUID ungradedId = UUID.randomUUID();
        givenApprovedRoster(student(), other(ungradedId));
        when(gradeRepository.findByCourseIdOrderByGradedAtDesc(COURSE_ID)).thenReturn(List.of(
                grade(new BigDecimal("18.00"), new BigDecimal("20.00"), new BigDecimal("40.00"))));

        var gradebook = gradeService.courseGradebook(COURSE_ID, TRAINER_ID, false);

        assertThat(gradebook.students()).hasSize(2);
        assertThat(gradebook.students().get(0).weightedAverage()).isEqualTo(90.0);
        assertThat(gradebook.students().get(1).grades()).isEmpty();
        assertThat(gradebook.students().get(1).weightedAverage()).isNull();
    }

    private void givenCourse() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    }

    private void givenApprovedRoster(User... students) {
        when(enrollmentRepository.findByCourseIdAndStatus(COURSE_ID, EnrollmentStatus.APPROVED))
                .thenReturn(List.of(students).stream()
                        .map(student -> Enrollment.builder()
                                .id(UUID.randomUUID())
                                .student(student)
                                .course(course())
                                .status(EnrollmentStatus.APPROVED)
                                .enrolledAt(Instant.EPOCH)
                                .build())
                        .toList());
    }

    private static GradeRequest request(BigDecimal score, BigDecimal maxScore, BigDecimal weight) {
        return new GradeRequest(STUDENT_ID, COURSE_ID, AssessmentType.EXAM, "Midterm", score, maxScore, weight, null);
    }

    private static Grade grade(BigDecimal score, BigDecimal maxScore, BigDecimal weight) {
        return Grade.builder()
                .id(GRADE_ID)
                .student(student())
                .course(course())
                .assessmentType(AssessmentType.EXAM)
                .title("Midterm")
                .score(score)
                .maxScore(maxScore)
                .weight(weight)
                .gradedBy(trainer())
                .gradedAt(Instant.EPOCH)
                .build();
    }

    private static Course course() {
        return Course.builder()
                .id(COURSE_ID).code("JAVA-101").name("Java Fundamentals")
                .durationHours(40).capacity(20).price(BigDecimal.TEN)
                .primaryTrainer(trainer())
                .status(CourseStatus.PUBLISHED)
                .build();
    }

    private static User trainer() {
        return user(TRAINER_ID, "Tina", "Trainer", Role.TRAINER);
    }

    private static User student() {
        return user(STUDENT_ID, "Ada", "Attendee", Role.STUDENT);
    }

    private static User other(UUID id) {
        return user(id, "Zoe", "Zero", Role.STUDENT);
    }

    private static User user(UUID id, String firstName, String lastName, Role role) {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .email(firstName.toLowerCase() + "@tcm.local")
                .passwordHash("hash")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
