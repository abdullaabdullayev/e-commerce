package com.example.authservice.controller;

import com.example.authservice.service.ParentInvitationService;
import com.example.authservice.dto.ParentInvitationDtos.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ParentInvitationController {
    private final ParentInvitationService invitations;

    @PostMapping("/api/v1/admin/parent-invitations")
    public ResponseEntity<Issued> invite(@Valid @RequestBody Invite request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(invitations.issue(request.email()));
    }

    @PostMapping("/api/v1/auth/parent-invitations/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@Valid @RequestBody Acceptance request) {
        invitations.accept(request.token(), request.firstName(), request.lastName(), request.password());
    }
}
