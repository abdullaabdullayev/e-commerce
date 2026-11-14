package com.example.courseservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lessons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lesson {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, updatable = false)
    private Course course;
    @Column(nullable = false)
    private int position;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(nullable = false, columnDefinition = "text")
    private String objective;
    @Column(nullable = false)
    private int durationMinutes;
    @Column(nullable = false, columnDefinition = "text")
    private String teacherNotes;
    @Column(nullable = false)
    private boolean archived;

    public Lesson(Course course, int position, String title, String objective, int durationMinutes, String teacherNotes) {
        id = UUID.randomUUID();
        this.course = course;
        this.position = position;
        revise(title, objective, durationMinutes, teacherNotes);
    }

    public void revise(String title, String objective, int durationMinutes, String teacherNotes) {
        this.title = title.trim();
        this.objective = objective;
        this.durationMinutes = durationMinutes;
        this.teacherNotes = teacherNotes;
    }

    public void reorder(int position) {
        this.position = position;
    }

    public void archive() {
        archived = true;
    }
}
