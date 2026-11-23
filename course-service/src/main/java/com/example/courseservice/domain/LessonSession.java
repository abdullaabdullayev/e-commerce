package com.example.courseservice.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lesson_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonSession {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Lesson lesson;
    private long teacherAccountId;
    private Instant heldAt;
    @Column(length = 160)
    private String topic;

    public LessonSession(Lesson lesson, long teacherId, Instant heldAt) {
        this.id = UUID.randomUUID();
        this.lesson = lesson;
        this.teacherAccountId = teacherId;
        this.heldAt = heldAt;
        this.topic = lesson.getTitle();
    }
}
