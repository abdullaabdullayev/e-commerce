package com.example.userservice.controller;

import com.example.userservice.dto.ProfileDtos.*;
import com.example.userservice.service.ProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profiles;

    @GetMapping("/api/v1/users/me")
    public Profile me(@AuthenticationPrincipal Jwt jwt) {
        return profiles.get(((Number) jwt.getClaim("accountId")).longValue());
    }

    @GetMapping("/api/v1/users/{accountId}")
    public Profile get(@PathVariable @Positive long accountId) { return profiles.get(accountId); }

    @PutMapping("/api/v1/users/{accountId}/guardian")
    public Profile guardian(@PathVariable @Positive long accountId, @Valid @RequestBody Guardian request) {
        return profiles.updateGuardian(accountId, request);
    }

    @PostMapping("/internal/v1/users/lookup")
    public List<Profile> lookup(@RequestHeader(value = "X-Profile-Service-Key", required = false) String supplied,
                                @Valid @RequestBody Lookup request) {
        return profiles.lookup(supplied, request.accountIds());
    }
}
