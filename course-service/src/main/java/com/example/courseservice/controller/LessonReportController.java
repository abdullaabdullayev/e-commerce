package com.example.courseservice.controller;

import com.example.courseservice.service.LessonReportService;

import com.example.courseservice.dto.PageResponse;
import com.example.courseservice.security.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static com.example.courseservice.dto.ReportDtos.*;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/sessions")
@RequiredArgsConstructor
@Validated
public class LessonReportController {
    private final LessonReportService reports;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionView record(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
                              @Valid @RequestBody NewSession request) {
        return reports.recordSession(Actor.from(jwt), courseId, request);
    }

    @GetMapping
    public PageResponse<SessionView> sessions(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
                                             @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
        return reports.sessions(Actor.from(jwt), courseId, PageRequest.of(page, 50));
    }

    @GetMapping("/{sessionId}/reports")
    public PageResponse<ReportView> reports(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
                                            @PathVariable UUID sessionId,
                                            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
        return reports.drafts(Actor.from(jwt), courseId, sessionId, PageRequest.of(page, 50));
    }

    @PutMapping("/{sessionId}/reports/{studentId}")
    public ReportView save(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
                           @PathVariable UUID sessionId, @PathVariable @Positive long studentId,
                           @Valid @RequestBody Draft draft) {
        return reports.saveDraft(Actor.from(jwt), courseId, sessionId, studentId, draft);
    }

    @GetMapping("/{sessionId}/reports/{studentId}")
    public ResponseEntity<ReportView> studentDraft(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID courseId, @PathVariable UUID sessionId, @PathVariable @Positive long studentId) {
        return reports.studentDraft(Actor.from(jwt), courseId, sessionId, studentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{sessionId}/reports/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void publish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId, @PathVariable UUID sessionId,
                        @Valid @RequestBody Publish request) {
        reports.publish(Actor.from(jwt), courseId, sessionId, request);
    }
}
