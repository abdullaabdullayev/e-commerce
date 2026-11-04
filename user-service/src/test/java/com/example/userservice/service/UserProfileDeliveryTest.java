package com.example.userservice.service;

import com.example.user.api.event.UserRegisteredEvent;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserService.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UserProfileDeliveryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired UserService service;
    @Autowired UserRepository users;

    @Test
    void concurrentRedeliveryCreatesOnlyOneProfile() throws Exception {
        var event = new UserRegisteredEvent(41L, "duplicate@example.test", "Test", "Parent");
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); service.createProfileIfAbsent(event); return null; });
            var second = executor.submit(() -> { start.await(); service.createProfileIfAbsent(event); return null; });
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }
        assertThat(users.findAll().stream().filter(user -> user.getAccountId().equals(41L)).count()).isEqualTo(1);
        assertThat(users.findByAccountId(41L)).isPresent();
    }

    @Test
    void conflictingEmailForAnotherAccountIsNotSilentlyDiscarded() {
        service.createProfileIfAbsent(new UserRegisteredEvent(51L, "conflict@example.test", "First", "Parent"));
        assertThatThrownBy(() -> service.createProfileIfAbsent(
                new UserRegisteredEvent(52L, "conflict@example.test", "Second", "Parent")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(users.findByAccountId(52L)).isEmpty();
    }
}
