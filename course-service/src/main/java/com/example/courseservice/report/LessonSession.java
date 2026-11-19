package com.example.courseservice.report;

import com.example.courseservice.domain.Lesson;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lesson_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class LessonSession {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Lesson lesson;
    private long teacherAccountId;
    private Instant heldAt;
    @Column(length = 160)
    private String topic;

    LessonSession(Lesson lesson, long teacherId, Instant heldAt) {
        this.id = UUID.randomUUID();
        this.lesson = lesson;
        this.teacherAccountId = teacherId;
        this.heldAt = heldAt;
        this.topic = lesson.getTitle();
    }
}
