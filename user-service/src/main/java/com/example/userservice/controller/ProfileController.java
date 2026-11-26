package com.example.userservice.controller;

import com.example.userservice.dto.ProfileDtos.*;
import com.example.userservice.service.ProfileService;
import jakarta.validation.Valid;
import com.example.userservice.messaging.RegistrationDlqReplayer;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    private final RegistrationDlqReplayer replayer;

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

    // Admin-only (ProfileSecurity): failed registration events awaiting a profile.
    @GetMapping("/api/v1/users/registration-dlq")
    public RegistrationDlqReplayer.Backlog registrationBacklog() { return replayer.backlog(); }

    @PostMapping("/api/v1/users/registration-dlq/replay")
    public RegistrationDlqReplayer.Replayed replayRegistrations(
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) int max) {
        return replayer.replay(max);
    }

    @PostMapping("/internal/v1/users/lookup")
    public List<Profile> lookup(@RequestHeader(value = "X-Profile-Service-Key", required = false) String supplied,
                                @Valid @RequestBody Lookup request) {
        return profiles.lookup(supplied, request.accountIds());
    }
}
