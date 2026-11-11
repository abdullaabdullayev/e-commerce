package com.example.authservice.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
public class LoginRequest {
    @NotBlank @Email @Size(max = 254)
    private String email;
    @NotBlank @Size(max = 72)
    private String password;

    @AssertTrue(message = "Password must not exceed 72 UTF-8 bytes")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
