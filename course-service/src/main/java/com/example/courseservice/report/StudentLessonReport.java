package com.example.courseservice.report;

import com.example.courseservice.domain.Enrollment;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
import static com.example.courseservice.exception.DomainException.conflict;

@Entity
@Table(name = "student_lesson_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class StudentLessonReport {
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

    StudentLessonReport(LessonSession session, Enrollment enrollment) {
        this.id = UUID.randomUUID();
        this.session = session;
        this.enrollment = enrollment;
    }

    void revise(ReportDtos.Draft draft) {
        if (publishedAt != null) throw conflict("The report has already been published");
        attended = draft.attended();
        learned = attended ? draft.learned().strip() : "";
        participation = attended ? draft.participation().strip() : "";
        recommendation = draft.recommendation().strip();
    }

    void publish(Instant now) {
        if (publishedAt != null) return;
        if (attended && (learned.isBlank() || participation.isBlank())) {
            throw conflict("Complete the learning and participation notes before publishing");
        }
        publishedAt = now;
    }

    ReportDtos.ReportView view() {
        var course = enrollment.getCourse();
        return new ReportDtos.ReportView(id, enrollment.getStudentAccountId(), course.getId(), course.getTitle(),
                session.getId(), session.getTopic(), session.getHeldAt(), attended,
                learned, participation, recommendation, publishedAt);
    }
}
