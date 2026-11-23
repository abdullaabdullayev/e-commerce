package com.example.authservice.dto;

import com.example.authservice.model.Account;
import com.example.authservice.model.Role;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class AccountDtos {
    private AccountDtos() {}

    public record AccountView(long accountId, String email, String username, boolean active, Set<Role> roles) {
        public static AccountView from(Account account) {
            return new AccountView(account.getId(), account.getEmail(), account.getUsername(), account.isActive(),
                    Set.copyOf(account.getRoles()));
        }
    }

    public record RolesInput(@NotEmpty Set<@NotNull Role> roles) {}
    public record BootstrapInput(@Positive long accountId) {}
    public record CreateAccount(@Email @Size(max = 254) String email,
                                @Pattern(regexp = "[a-z][a-z0-9._-]{2,39}") String username,
                                @NotBlank @Size(min = 8, max = 72) String password,
                                @NotBlank @Size(max = 80) String firstName, @NotBlank @Size(max = 80) String lastName,
                                @NotEmpty Set<@NotNull Role> roles) {
        @AssertTrue public boolean isIdentityValid() { return (email != null && !email.isBlank()) || username != null; }
        @AssertTrue public boolean isPasswordWithinByteLimit() {
            return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
        }
    }
}
