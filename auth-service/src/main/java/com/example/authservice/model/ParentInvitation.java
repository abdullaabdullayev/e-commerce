package com.example.authservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "parent_invitations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParentInvitation {
    @Id @Column(length = 64)
    private String tokenHash;
    @Getter @Column(nullable = false, updatable = false, length = 254)
    private String email;
    @Getter @Column(nullable = false, updatable = false)
    private Instant expiresAt;
    private Instant acceptedAt;

    public ParentInvitation(String tokenHash, String email, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.email = email;
        this.expiresAt = expiresAt;
    }

    public boolean usableAt(Instant now) {
        return acceptedAt == null && expiresAt.isAfter(now);
    }

    public void accept(Instant now) {
        acceptedAt = now;
    }
}
