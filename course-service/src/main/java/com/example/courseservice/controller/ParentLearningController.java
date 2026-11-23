package com.example.courseservice.controller;

import com.example.courseservice.dto.LearningDtos.LearningResponse;
import com.example.courseservice.dto.PageResponse;
import com.example.courseservice.dto.PointDtos.PointHistory;
import com.example.courseservice.service.ParentLearningService;
import com.example.courseservice.dto.ReportDtos.ReportView;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parent/children/{studentId}")
@RequiredArgsConstructor
@Validated
public class ParentLearningController {
    private final ParentLearningService learning;

    @GetMapping("/courses")
    public PageResponse<LearningResponse> courses(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive long studentId,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return learning.courses(jwt.getTokenValue(), studentId, page, size);
    }

    @GetMapping("/courses/{courseId}/points")
    public PointHistory history(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive long studentId,
            @PathVariable UUID courseId, @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return learning.history(jwt.getTokenValue(), courseId, studentId, page, size);
    }

    @GetMapping("/reports")
    public PageResponse<ReportView> reports(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive long studentId,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return learning.reports(jwt.getTokenValue(), studentId, PageRequest.of(page, size));
    }
}
