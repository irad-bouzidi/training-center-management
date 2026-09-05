package com.tcm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full Spring context against a real, ephemeral Postgres
 * (Testcontainers) and proves the Liquibase pipeline works end to end:
 * bookkeeping tables get created and the extension changeset is applied.
 *
 * This is the pattern every future domain integration test should follow
 * (see TCM-3): annotate with {@link Testcontainers}, declare a
 * {@link Container} with {@link ServiceConnection} so Spring Boot wires the
 * datasource automatically, and activate the "test" profile.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class LiquibaseMigrationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsAndLiquibaseAppliesMigrations() {
        Integer changeSetCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog WHERE id = ?",
                Integer.class,
                "20260101-01-init-extensions");

        assertThat(changeSetCount).isEqualTo(1);

        Integer extensionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'pgcrypto'",
                Integer.class);

        assertThat(extensionCount).isEqualTo(1);
    }
}
