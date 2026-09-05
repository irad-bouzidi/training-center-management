package com.tcm.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcm.course.model.Course;
import com.tcm.course.model.CourseStatus;
import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Repository-layer test against a real, ephemeral Postgres (Testcontainers) -
 * same pattern as {@code UserRepositoryIT} (see TCM-8).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class EnrollmentRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User persistUser(String email, Role role) {
        return entityManager.persistAndFlush(User.builder()
                .firstName("Jane").lastName("Doe").email(email).passwordHash("hash")
                .role(role).status(UserStatus.ACTIVE)
                .build());
    }

    private Course persistCourse(String code) {
        return entityManager.persistAndFlush(Course.builder()
                .code(code).name("Course " + code).durationHours(40).capacity(20)
                .price(BigDecimal.valueOf(500)).status(CourseStatus.PUBLISHED)
                .build());
    }

    @Test
    void existsFindByStudentAndCourse_reflectPersistedRows() {
        User student = persistUser("student@example.com", Role.STUDENT);
        Course course = persistCourse("JAVA-101");
        entityManager.persistAndFlush(Enrollment.builder()
                .student(student).course(course).status(EnrollmentStatus.PENDING).build());

        assertThat(enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())).isTrue();
        assertThat(enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), java.util.UUID.randomUUID()))
                .isFalse();
        assertThat(enrollmentRepository.findByStudentId(student.getId())).hasSize(1);
        assertThat(enrollmentRepository.findByCourseId(course.getId())).hasSize(1);
        assertThat(enrollmentRepository.existsByCourseId(course.getId())).isTrue();
    }

    @Test
    void countByCourseIdAndStatus_countsOnlyMatchingStatus() {
        User student1 = persistUser("student1@example.com", Role.STUDENT);
        User student2 = persistUser("student2@example.com", Role.STUDENT);
        Course course = persistCourse("JAVA-102");
        entityManager.persistAndFlush(Enrollment.builder()
                .student(student1).course(course).status(EnrollmentStatus.APPROVED).build());
        entityManager.persistAndFlush(Enrollment.builder()
                .student(student2).course(course).status(EnrollmentStatus.PENDING).build());

        assertThat(enrollmentRepository.countByCourseIdAndStatus(course.getId(), EnrollmentStatus.APPROVED))
                .isEqualTo(1);
        assertThat(enrollmentRepository.countByStudentIdAndStatus(student1.getId(), EnrollmentStatus.APPROVED))
                .isEqualTo(1);
    }

    @Test
    void duplicateStudentCoursePair_violatesUniqueConstraint() {
        User student = persistUser("dup@example.com", Role.STUDENT);
        Course course = persistCourse("JAVA-103");
        entityManager.persistAndFlush(Enrollment.builder()
                .student(student).course(course).status(EnrollmentStatus.PENDING).build());

        Enrollment duplicate = Enrollment.builder()
                .student(student).course(course).status(EnrollmentStatus.PENDING).build();

        // Goes through the repository (not TestEntityManager) so Spring's
        // exception translation actually applies - it only wraps exceptions
        // thrown from proxied @Repository methods.
        assertThatThrownBy(() -> enrollmentRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
