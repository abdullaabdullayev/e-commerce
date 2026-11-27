package com.example.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Boots the full application context against a disposable PostgreSQL, so Flyway migrations and JPA mappings are verified together. */
@SpringBootTest(properties = {
        "spring.jwt.secret=test-secret-key-that-is-at-least-32-bytes-long",
        "spring.jwt.access-expiration=900000",
        "spring.jwt.refresh-expiration=604800000",
        "spring.rabbitmq.username=guest",
        "spring.rabbitmq.password=guest",
        "eureka.client.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class AuthServiceApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void contextLoads() {
    }
}
