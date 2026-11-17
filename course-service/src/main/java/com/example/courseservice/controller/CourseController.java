package com.example.courseservice.controller;

import com.example.courseservice.security.Actor;
import com.example.courseservice.dto.PageResponse;
import com.example.courseservice.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static com.example.courseservice.dto.CourseDtos.*;
import static com.example.courseservice.dto.LearningDtos.*;

@RestController
@Validated
public class CourseController {
    private final CourseService courses;
    private final MembershipService memberships;
    private final LearningService learning;

    public CourseController(CourseService courses, MembershipService memberships, LearningService learning) {
        this.courses = courses;
        this.memberships = memberships;
        this.learning = learning;
    }

    @GetMapping("/api/v1/courses")
    public PageResponse<CourseResponse> list(@RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
                                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return courses.published(page, size);
    }

    @GetMapping("/api/v1/courses/{id}")
    public CourseResponse get(@PathVariable UUID id) {
        return courses.published(id);
    }

    @GetMapping({"/api/v1/courses/mine", "/api/v1/admin/courses"})
    public PageResponse<CourseResponse> managed(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return courses.managed(Actor.from(jwt), page, size);
    }

    @GetMapping("/api/v1/courses/my-learning")
    public PageResponse<LearningResponse> learning(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return learning.mine(Actor.from(jwt), page, size);
    }

    @PostMapping({"/api/v1/courses", "/api/v1/admin/courses"})
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CourseRequest request) {
        return courses.create(Actor.from(jwt), request);
    }

    @PutMapping({"/api/v1/courses/{id}", "/api/v1/admin/courses/{id}"})
    public CourseResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @Valid @RequestBody CourseRequest request) {
        return courses.update(Actor.from(jwt), id, request);
    }

    @PutMapping({"/api/v1/courses/{id}/status", "/api/v1/admin/courses/{id}/status"})
    public CourseResponse status(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @Valid @RequestBody StatusRequest request) {
        return courses.changeStatus(Actor.from(jwt), id, request.status());
    }

    @PutMapping({"/api/v1/courses/{id}/teachers/{accountId}", "/api/v1/admin/courses/{id}/teachers/{accountId}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignTeacher(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                              @PathVariable @Positive long accountId) {
        memberships.assignTeacher(Actor.from(jwt), jwt.getTokenValue(), id, accountId);
    }

    @DeleteMapping({"/api/v1/courses/{id}/teachers/{accountId}", "/api/v1/admin/courses/{id}/teachers/{accountId}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTeacher(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                              @PathVariable @Positive long accountId) {
        memberships.removeTeacher(Actor.from(jwt), id, accountId);
    }

    @PutMapping({"/api/v1/courses/{id}/students/{accountId}", "/api/v1/admin/courses/{id}/students/{accountId}"})
    public EnrollmentResponse enroll(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                     @PathVariable @Positive long accountId, @Valid @RequestBody EnrollmentRequest request) {
        return memberships.enroll(Actor.from(jwt), jwt.getTokenValue(), id, accountId, request.status());
    }

    @GetMapping({"/api/v1/courses/{id}/students", "/api/v1/admin/courses/{id}/students"})
    public RosterResponse roster(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
        return memberships.roster(Actor.from(jwt), id, page);
    }
}
