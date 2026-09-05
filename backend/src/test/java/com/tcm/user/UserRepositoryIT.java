package com.tcm.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.util.Optional;
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
 * same pattern as {@code LiquibaseMigrationIT} (see TCM-3), but scoped to
 * just the JPA layer via {@code @DataJpaTest}. {@code Replace.NONE} stops
 * Spring from swapping in an embedded database, so this still exercises the
 * real users table + constraints created by Liquibase. Named *IT (not
 * *Test) so it runs under failsafe/verify, not surefire/test - it needs
 * Docker, like every other Testcontainers-backed test in this project.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class UserRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static User newUser(String email) {
        return User.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email(email)
                .passwordHash("hashed")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void findByEmail_returnsPersistedUser() {
        entityManager.persistAndFlush(newUser("jane.doe@example.com"));

        Optional<User> found = userRepository.findByEmail("jane.doe@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getLastName()).isEqualTo("Doe");
    }

    @Test
    void findByEmail_unknownEmail_returnsEmpty() {
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void existsByEmail_reflectsPersistedRows() {
        entityManager.persistAndFlush(newUser("john.smith@example.com"));

        assertThat(userRepository.existsByEmail("john.smith@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nope@example.com")).isFalse();
    }

    @Test
    void duplicateEmail_violatesUniqueConstraint() {
        entityManager.persistAndFlush(newUser("dup@example.com"));

        User second = newUser("dup@example.com");

        // Goes through the repository (not TestEntityManager) so Spring's
        // exception translation actually applies - it only wraps exceptions
        // thrown from proxied @Repository methods.
        assertThatThrownBy(() -> userRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
