package com.example.courseservice.controller;

import com.example.courseservice.dto.LearningDtos.LearningResponse;
import com.example.courseservice.dto.PageResponse;
import com.example.courseservice.dto.PointDtos.PointHistory;
import com.example.courseservice.integration.ProfileClient;
import com.example.courseservice.report.LessonReportService;
import com.example.courseservice.report.ReportDtos.ReportView;
import com.example.courseservice.service.LearningService;
import com.example.courseservice.service.PointService;
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
    private final ProfileClient profiles;
    private final LearningService learning;
    private final PointService points;
    private final LessonReportService reports;

    @GetMapping("/courses")
    public PageResponse<LearningResponse> courses(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive long studentId,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        profiles.requireParentLink(jwt.getTokenValue(), studentId);
        return learning.forStudent(studentId, page, size);
    }

    @GetMapping("/courses/{courseId}/points")
    public PointHistory history(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive long studentId,
            @PathVariable UUID courseId, @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        profiles.requireParentLink(jwt.getTokenValue(), studentId);
        return points.familyHistory(courseId, studentId, page, size);
    }

    @GetMapping("/reports")
    public PageResponse<ReportView> reports(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive long studentId,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        profiles.requireParentLink(jwt.getTokenValue(), studentId);
        return reports.publishedForChild(studentId, PageRequest.of(page, size));
    }
}
