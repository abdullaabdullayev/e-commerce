package com.example.authservice.service;

import com.example.authservice.model.Account;
import com.example.authservice.model.RefreshToken;
import com.example.authservice.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/** Refresh tokens are single-use and stored only as SHA-256 digests. */
@Service
public class RefreshTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final long expirationMs;

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${spring.jwt.refresh-expiration}") long expirationMs) {
        this.repository = repository;
        this.expirationMs = expirationMs;
    }

    /** Stores a new token and returns its raw value, which is never persisted. */
    @Transactional
    public String issue(Account account) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(RefreshToken.builder().token(digest(raw)).account(account)
                .expiryDate(Instant.now().plusMillis(expirationMs)).build());
        return raw;
    }

    /** Consumes a token (rotation): it can be used once, then it is gone. */
    @Transactional
    public Account consume(String raw) {
        RefreshToken token = repository.lockByToken(digest(raw)).orElseThrow(RefreshTokenService::invalid);
        repository.delete(token);
        if (token.getExpiryDate().isBefore(Instant.now()) || !token.getAccount().isActive()) throw invalid();
        return token.getAccount();
    }

    @Transactional
    public void revoke(String raw) {
        repository.deleteByToken(digest(raw));
    }

    @Transactional
    public void revokeAll(long accountId) {
        repository.deleteByAccountId(accountId);
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    @Transactional
    public void purgeExpired() {
        repository.deleteExpired(Instant.now());
    }

    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired");
    }

    static String digest(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
