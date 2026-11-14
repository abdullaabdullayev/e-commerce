package com.example.courseservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {
    @Id
    private UUID id;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(nullable = false, columnDefinition = "text")
    private String description;
    @Column(nullable = false, length = 80)
    private String level;
    @Column(nullable = false)
    private int minAge;
    @Column(nullable = false)
    private int maxAge;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Course(String title, String description, String level, int minAge, int maxAge) {
        id = UUID.randomUUID();
        createdAt = Instant.now();
        status = CourseStatus.DRAFT;
        revise(title, description, level, minAge, maxAge);
    }

    public void revise(String title, String description, String level, int minAge, int maxAge) {
        this.title = title.trim();
        this.description = description;
        this.level = level.trim();
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    public void changeStatus(CourseStatus status) {
        this.status = status;
    }
}
