package com.example.courseservice.dto;

import com.example.courseservice.domain.*;
import static com.example.courseservice.dto.CourseDtos.*;
import static com.example.courseservice.dto.LearningDtos.*;
import static com.example.courseservice.dto.MaterialDtos.*;
import static com.example.courseservice.dto.PointDtos.*;

public final class CourseMapper {
    private CourseMapper() {}

    public static CourseResponse course(Course value) {
        return new CourseResponse(value.getId(), value.getTitle(), value.getDescription(), value.getLevel(),
                value.getMinAge(), value.getMaxAge(), value.getStatus(), value.getCreatedAt());
    }

    public static LessonResponse lesson(Lesson value) {
        return new LessonResponse(value.getId(), value.getCourse().getId(), value.getPosition(), value.getTitle(),
                value.getObjective(), value.getDurationMinutes(), value.getTeacherNotes(), value.isArchived());
    }

    public static EnrollmentResponse enrollment(Enrollment value) {
        return new EnrollmentResponse(value.getId(), value.getCourse().getId(), value.getStudentAccountId(),
                value.getStatus(), value.getEnrolledAt());
    }

    public static MaterialResponse material(Material value) {
        return new MaterialResponse(value.getId(), value.getTitle(), value.getKind(), value.getFilename(),
                value.getContentType(), value.getSizeBytes(), value.getUrl());
    }

    public static PointResponse point(StudentPoint value) {
        return new PointResponse(value.getId(), value.getRequestId(), value.getType(), value.getPoints(),
                value.getRequestedPoints(), value.getReason(), value.getAwardedByAccountId(),
                value.getOriginalAward() == null ? null : value.getOriginalAward().getId(), value.getCreatedAt());
    }
}
