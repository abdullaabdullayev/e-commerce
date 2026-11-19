package com.example.authservice.invitation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class ParentInvitationController {
    private final ParentInvitationService invitations;

    public record Invite(@NotBlank @Email @Size(max = 254) String email) {}
    public record Acceptance(@NotNull @Pattern(regexp = "[0-9a-f]{64}") String token,
                             @NotBlank @Size(max = 80) String firstName, @NotBlank @Size(max = 80) String lastName,
                             @NotBlank @Size(min = 8, max = 72) String password) {
        @AssertTrue public boolean isPasswordWithinByteLimit() {
            return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
        }
    }

    @PostMapping("/api/v1/admin/parent-invitations")
    public ResponseEntity<ParentInvitationService.Issued> invite(@Valid @RequestBody Invite request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(invitations.issue(request.email()));
    }

    @PostMapping("/api/v1/auth/parent-invitations/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@Valid @RequestBody Acceptance request) {
        invitations.accept(request.token(), request.firstName(), request.lastName(), request.password());
    }
}
