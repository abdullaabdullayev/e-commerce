package com.example.courseservice.controller;

import com.example.courseservice.security.Actor;
import com.example.courseservice.service.PointService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static com.example.courseservice.dto.PointDtos.*;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/students")
@Validated
public class PointController {
    private final PointService points;

    public PointController(PointService points) {
        this.points = points;
    }

    @PostMapping("/{studentId}/points")
    @ResponseStatus(HttpStatus.CREATED)
    public PointResponse award(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
                               @PathVariable @Positive long studentId, @Valid @RequestBody AwardRequest request) {
        return points.award(Actor.from(jwt), courseId, studentId, request);
    }

    @PostMapping("/{studentId}/points/{awardId}/corrections")
    @ResponseStatus(HttpStatus.CREATED)
    public PointResponse correct(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
                                 @PathVariable @Positive long studentId, @PathVariable UUID awardId,
                                 @Valid @RequestBody CorrectionRequest request) {
        return points.correct(Actor.from(jwt), courseId, studentId, awardId, request);
    }

    @GetMapping("/{studentId}/points")
    public PointHistory history(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
                                @PathVariable @Positive long studentId,
                                @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return points.history(Actor.from(jwt), courseId, studentId, page, size);
    }

    @GetMapping("/me/points")
    public PointHistory mine(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
                             @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Actor actor = Actor.from(jwt);
        return points.history(actor, courseId, actor.id(), page, size);
    }
}
