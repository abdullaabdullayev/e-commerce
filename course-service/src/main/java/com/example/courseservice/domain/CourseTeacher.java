package com.example.courseservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_teachers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseTeacher {
    @EmbeddedId
    private CourseTeacherId id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("courseId")
    @JoinColumn(name = "course_id")
    private Course course;
    // Retained for V1 compatibility; it never grants syllabus editing in this MVP.
    @Column(nullable = false, length = 10)
    private String permission = "VIEW";
    @Column(nullable = false)
    private boolean active;

    public CourseTeacher(Course course, long accountId) {
        this.course = course;
        this.id = new CourseTeacherId(course.getId(), accountId);
        activate();
    }

    public void activate() {
        active = true;
        permission = "VIEW";
    }

    public void deactivate() {
        active = false;
    }
}
