package com.example.authservice.messaging;

import com.example.authservice.model.Account;
import com.example.authservice.repository.AccountRepository;
import com.example.authservice.repository.OutboxEventRepository;
import com.example.user.api.event.UserRegisteredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The disposable container removes the database; do not run DROP after it stops.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UserEventPublisher.class, OutboxPersistenceTest.JsonConfig.class})
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxPersistenceTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired AccountRepository accounts;
    @Autowired OutboxEventRepository outbox;
    @Autowired UserEventPublisher publisher;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void accountAndEventRollBackTogether() {
        long accountCount = accounts.count();
        long eventCount = outbox.count();
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Account account = createAccount("rollback@example.test");
            publisher.publishUserRegistered(new UserRegisteredEvent(account.getId(), account.getEmail(), "Test", "Parent"));
            outbox.flush();
            throw new IllegalStateException("simulate registration failure");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(accounts.count()).isEqualTo(accountCount);
        assertThat(outbox.count()).isEqualTo(eventCount);
    }

    @Test
    void committedEventIsSelectableAndPublishedEventIsNotSelectedAgain() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Account account = createAccount("commit@example.test");
            publisher.publishUserRegistered(new UserRegisteredEvent(account.getId(), account.getEmail(), "Test", "Parent"));
        });
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var event = outbox.lockNextPending().orElseThrow();
            assertThat(event.getPayload()).contains("commit@example.test");
            event.published();
        });
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                assertThat(outbox.lockNextPending()).isEmpty());
    }

    private Account createAccount(String email) {
        Account account = new Account();
        account.setEmail(email);
        account.setPasswordHash("test-hash");
        return accounts.saveAndFlush(account);
    }

    @TestConfiguration
    static class JsonConfig {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
    }
}
