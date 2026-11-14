package com.example.courseservice.controller;

import com.example.courseservice.security.Actor;
import com.example.courseservice.service.SyllabusService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static com.example.courseservice.dto.CourseDtos.*;

@RestController
@RequestMapping("/api/v1/courses/{courseId}")
public class SyllabusController {
    private final SyllabusService syllabus;

    public SyllabusController(SyllabusService syllabus) {
        this.syllabus = syllabus;
    }

    @GetMapping("/syllabus")
    public SyllabusResponse read(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId) {
        return syllabus.read(Actor.from(jwt), courseId);
    }

    @PostMapping("/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public LessonResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
                                 @Valid @RequestBody LessonRequest request) {
        return syllabus.create(Actor.from(jwt), courseId, request);
    }

    @PutMapping("/lessons/{lessonId}")
    public LessonResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
                                 @PathVariable UUID lessonId, @Valid @RequestBody LessonRequest request) {
        return syllabus.update(Actor.from(jwt), courseId, lessonId, request);
    }

    @PutMapping("/lesson-order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
                        @Valid @RequestBody LessonOrderRequest request) {
        syllabus.reorder(Actor.from(jwt), courseId, request.lessonIds());
    }

    @DeleteMapping("/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId, @PathVariable UUID lessonId) {
        syllabus.archive(Actor.from(jwt), courseId, lessonId);
    }
}
