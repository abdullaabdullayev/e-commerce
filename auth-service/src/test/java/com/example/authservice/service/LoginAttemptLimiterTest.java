package com.example.authservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptLimiterTest {
    private static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

    @Test
    void blocksAfterRepeatedFailuresCaseInsensitively() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) limiter.failed("User@Example.com");
        assertThatThrownBy(() -> limiter.check("user@example.com"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("429");
    }

    @Test
    void allowsFewerFailuresAndResetsOnSuccess() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES - 1; i++) limiter.failed("a@x.az");
        assertThatCode(() -> limiter.check("a@x.az")).doesNotThrowAnyException();
        limiter.succeeded("a@x.az");
        limiter.failed("a@x.az");
        assertThatCode(() -> limiter.check("a@x.az")).doesNotThrowAnyException();
    }

    @Test
    void blockExpiresAfterWindow() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) limiter.failed("b@x.az");
        clock.now = clock.now.plus(LoginAttemptLimiter.WINDOW).plus(Duration.ofSeconds(1));
        assertThatCode(() -> limiter.check("b@x.az")).doesNotThrowAnyException();
        limiter.purge();
    }

    @Test
    void failuresOutsideTheWindowDoNotAccumulate() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES - 1; i++) limiter.failed("c@x.az");
        clock.now = clock.now.plus(LoginAttemptLimiter.WINDOW).plus(Duration.ofSeconds(1));
        limiter.failed("c@x.az");
        assertThatCode(() -> limiter.check("c@x.az")).doesNotThrowAnyException();
    }

    @Test
    void emailsAreNormalized() {
        org.assertj.core.api.Assertions.assertThat(Emails.normalize("  Ali@Example.COM ")).isEqualTo("ali@example.com");
        org.assertj.core.api.Assertions.assertThat(Emails.normalize(" ")).isNull();
    }
}
