package com.example.authservice.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;

@Data
public class RegisterRequest {

    @NotBlank @Email @Size(max = 254)
    private String email;
    @NotBlank @Size(min = 8, max = 72)
    private String password;
    @NotBlank @Size(max = 80)
    private String firstName;
    @NotBlank @Size(max = 80)
    private String lastName;

    @AssertTrue(message = "Password must not exceed 72 UTF-8 bytes")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
