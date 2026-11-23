package com.example.userservice.dto;

import com.example.userservice.model.User;
import jakarta.validation.constraints.*;
import java.util.List;

public final class ProfileDtos {
    private ProfileDtos() {}
    public record Profile(long accountId, String firstName, String lastName, String guardianName, String guardianContact) {
        public static Profile from(User user) {
            return new Profile(user.getAccountId(), user.getFirstName(), user.getLastName(),
                    user.getGuardianName(), user.getGuardianContact());
        }
    }
    public record Lookup(@NotEmpty @Size(max = 50) List<@NotNull @Positive Long> accountIds) {}
    public record Guardian(@NotBlank @Size(max = 160) String name, @NotBlank @Size(max = 200) String contact) {}
}
