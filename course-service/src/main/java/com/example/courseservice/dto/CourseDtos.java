package com.example.courseservice.dto;

import com.example.courseservice.domain.CourseStatus;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class CourseDtos {
    private CourseDtos() {}

    public record CourseRequest(
            @NotBlank @Size(max = 160) String title,
            @NotNull @Size(max = 10000) String description,
            @NotBlank @Size(max = 80) String level,
            @Min(3) @Max(100) int minAge,
            @Min(3) @Max(100) int maxAge) {}

    public record StatusRequest(@NotNull CourseStatus status) {}

    public record CourseResponse(UUID id, String title, String description, String level,
                                 int minAge, int maxAge, CourseStatus status, Instant createdAt) {}

    public record LessonRequest(
            @NotBlank @Size(max = 160) String title,
            @NotNull @Size(max = 10000) String objective,
            @Min(1) @Max(600) int durationMinutes,
            @NotNull @Size(max = 50000) String teacherNotes) {}

    public record LessonOrderRequest(@NotEmpty @Size(max = 500) List<@NotNull UUID> lessonIds) {}

    public record LessonResponse(UUID id, UUID courseId, int position, String title, String objective,
                                 int durationMinutes, String teacherNotes, boolean archived) {}

    public record LessonContent(LessonResponse lesson, List<MaterialDtos.MaterialResponse> materials) {}
    public record SyllabusResponse(CourseResponse course, List<Long> teacherAccountIds, List<LessonContent> lessons) {}
}
