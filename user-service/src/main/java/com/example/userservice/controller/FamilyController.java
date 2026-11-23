package com.example.userservice.controller;

import com.example.userservice.service.FamilyAssignmentService;
import com.example.userservice.service.FamilyService;
import com.example.userservice.dto.ChildProfile;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
public class FamilyController {
    private final FamilyAssignmentService assignments;
    private final FamilyService families;
    private static final String CHILDREN = "/api/v1/admin/parents/{parentId}/children";

    @PutMapping(CHILDREN + "/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void link(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive long parentId,
                     @PathVariable @Positive long studentId) {
        assignments.assign(jwt.getTokenValue(), parentId, studentId);
    }

    @DeleteMapping(CHILDREN + "/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@PathVariable @Positive long parentId, @PathVariable @Positive long studentId) {
        families.unlink(parentId, studentId);
    }

    @GetMapping(CHILDREN)
    public List<ChildProfile> linkedChildren(@PathVariable @Positive long parentId) {
        return families.children(parentId);
    }

    @GetMapping("/api/v1/parent/children")
    public List<ChildProfile> myChildren(@AuthenticationPrincipal Jwt jwt) {
        return families.children(((Number) jwt.getClaim("accountId")).longValue());
    }

    @GetMapping("/internal/v1/parent/children/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void checkAccess(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive long studentId) {
        families.requireLink(((Number) jwt.getClaim("accountId")).longValue(), studentId);
    }
}
