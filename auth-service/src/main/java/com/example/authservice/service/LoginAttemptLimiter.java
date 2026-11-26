package com.example.authservice.service;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blocks an identifier for a while after repeated failed logins.
 * State is per instance; with several Auth instances each one limits independently.
 */
@Component
public class LoginAttemptLimiter {
    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_TRACKED = 100_000;

    private record Attempts(int failures, Instant firstFailure, Instant blockedUntil) {}

    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptLimiter() { this(Clock.systemUTC()); }

    LoginAttemptLimiter(Clock clock) { this.clock = clock; }

    public void check(String identifier) {
        Attempts current = attempts.get(key(identifier));
        if (current != null && current.blockedUntil() != null && current.blockedUntil().isAfter(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts; try again later");
        }
    }

    public void failed(String identifier) {
        if (attempts.size() >= MAX_TRACKED) purge();
        Instant now = clock.instant();
        attempts.compute(key(identifier), (k, current) -> {
            if (current == null || current.firstFailure().plus(WINDOW).isBefore(now)) {
                current = new Attempts(0, now, null);
            }
            int failures = current.failures() + 1;
            return new Attempts(failures, current.firstFailure(), failures >= MAX_FAILURES ? now.plus(WINDOW) : null);
        });
    }

    public void succeeded(String identifier) {
        attempts.remove(key(identifier));
    }

    @Scheduled(fixedDelay = 600_000)
    public void purge() {
        Instant now = clock.instant();
        attempts.entrySet().removeIf(entry -> {
            Attempts value = entry.getValue();
            Instant expiry = value.blockedUntil() != null ? value.blockedUntil() : value.firstFailure().plus(WINDOW);
            return !expiry.isAfter(now);
        });
    }

    private static String key(String identifier) {
        return identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
    }
}
