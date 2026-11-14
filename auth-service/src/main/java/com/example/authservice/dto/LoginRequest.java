package com.example.authservice.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
public class LoginRequest {
    @Email @Size(max = 254)
    private String email;
    @Size(max = 254)
    private String login;
    @NotBlank @Size(max = 72)
    private String password;

    @AssertTrue(message = "Password must not exceed 72 UTF-8 bytes")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
    public String getIdentifier() { return login != null ? login.trim() : email == null ? "" : email.trim(); }
    @AssertTrue(message = "Supply either login or email")
    public boolean isLoginProvided() { return (login == null || email == null) && !getIdentifier().isBlank(); }
}
