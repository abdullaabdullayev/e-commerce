package com.example.authservice.invitation;

import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.repository.AccountRepository;
import com.example.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
class ParentInvitationService {
    private final ParentInvitationRepository invitations;
    private final AccountRepository accounts;
    private final AuthService auth;
    private static final SecureRandom RANDOM = new SecureRandom();
    record Issued(String token, Instant expiresAt) {}

    @Transactional
    public Issued issue(String email) {
        if (accounts.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Account already exists");
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        Instant expires = Instant.now().plus(Duration.ofHours(48));
        invitations.save(new ParentInvitation(digest(token), email, expires));
        return new Issued(token, expires);
    }

    @Transactional
    public void accept(String token, String firstName, String lastName, String password) {
        Instant now = Instant.now();
        var invitation = invitations.findForAcceptance(digest(token)).filter(i -> i.usableAt(now))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE, "Invitation expired or already used"));
        var registration = new RegisterRequest();
        registration.setEmail(invitation.getEmail());
        registration.setFirstName(firstName);
        registration.setLastName(lastName);
        registration.setPassword(password);
        auth.registerInvitedParent(registration);
        invitation.accept(now);
    }

    private static String digest(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
