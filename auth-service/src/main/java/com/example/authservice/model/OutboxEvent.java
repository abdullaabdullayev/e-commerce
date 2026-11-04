package com.example.authservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Durable UserRegistered delivery intent, committed with the account. */
@Entity
@Table(name = "auth_outbox", indexes = @Index(
        name = "idx_auth_outbox_pending", columnList = "published_at,next_attempt_at,created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {
    @Id
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    public static OutboxEvent pending(String payload) {
        OutboxEvent event = new OutboxEvent();
        event.id = UUID.randomUUID();
        event.payload = payload;
        event.createdAt = Instant.now();
        event.nextAttemptAt = event.createdAt;
        return event;
    }

    public void published() {
        attempts++;
        publishedAt = Instant.now();
    }

    public void retryLater() {
        attempts++;
        long delaySeconds = Math.min(300, 5L << Math.min(attempts - 1, 6));
        nextAttemptAt = Instant.now().plusSeconds(delaySeconds);
    }
}
