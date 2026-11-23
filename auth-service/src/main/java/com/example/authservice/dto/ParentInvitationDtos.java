package com.example.authservice.dto;

import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public final class ParentInvitationDtos {
    private ParentInvitationDtos() {}
    public record Issued(String token, Instant expiresAt) {}
    public record Invite(@NotBlank @Email @Size(max = 254) String email) {}
    public record Acceptance(@NotNull @Pattern(regexp = "[0-9a-f]{64}") String token,
                             @NotBlank @Size(max = 80) String firstName, @NotBlank @Size(max = 80) String lastName,
                             @NotBlank @Size(min = 8, max = 72) String password) {
        @AssertTrue public boolean isPasswordWithinByteLimit() {
            return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
        }
    }
}
