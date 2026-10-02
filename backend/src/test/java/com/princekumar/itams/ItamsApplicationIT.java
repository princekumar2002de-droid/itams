package com.princekumar.itams;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test: the Spring context loads, Flyway migrations apply, JPA
 * validates every entity against the schema, and Security wires up.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class ItamsApplicationIT {

    @Container
    static final PostgreSQLContainer<?> pg =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("itams_smoke").withUsername("itams").withPassword("itams");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @Test
    void contextLoads() {}
}
