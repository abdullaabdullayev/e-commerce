package com.example.courseservice.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
import static com.example.courseservice.exception.DomainException.conflict;

@Getter
@Entity
@Table(name = "student_lesson_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentLessonReport {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private LessonSession session;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Enrollment enrollment;
    private boolean attended;
    @Column(length = 2000) private String learned;
    @Column(length = 2000) private String participation;
    @Column(length = 1000) private String recommendation;
    private Instant publishedAt;

    public StudentLessonReport(LessonSession session, Enrollment enrollment) {
        this.id = UUID.randomUUID();
        this.session = session;
        this.enrollment = enrollment;
    }

    public void revise(boolean attended, String learned, String participation, String recommendation) {
        if (publishedAt != null) throw conflict("The report has already been published");
        this.attended = attended;
        this.learned = attended ? learned.strip() : "";
        this.participation = attended ? participation.strip() : "";
        this.recommendation = recommendation.strip();
    }

    public void publish(Instant now) {
        if (publishedAt != null) return;
        if (attended && (learned.isBlank() || participation.isBlank())) {
            throw conflict("Complete the learning and participation notes before publishing");
        }
        publishedAt = now;
    }

}
