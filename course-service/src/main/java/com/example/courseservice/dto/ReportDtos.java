package com.example.courseservice.dto;

import com.example.courseservice.domain.LessonSession;
import com.example.courseservice.domain.StudentLessonReport;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ReportDtos {
    private ReportDtos() {}

    public record NewSession(@NotNull UUID lessonId, @NotNull @PastOrPresent Instant heldAt) {}

    public record SessionView(UUID id, UUID lessonId, long teacherAccountId, Instant heldAt, String topic) {
        public static SessionView of(LessonSession session) {
            return new SessionView(session.getId(), session.getLesson().getId(), session.getTeacherAccountId(),
                    session.getHeldAt(), session.getTopic());
        }
    }

    public record Draft(@NotNull Boolean attended, @NotNull @Size(max = 2000) String learned,
                        @NotNull @Size(max = 2000) String participation,
                        @NotNull @Size(max = 1000) String recommendation) {}

    public record Publish(@NotEmpty @Size(max = 100) List<@NotNull @Positive Long> studentIds) {}

    public record ReportView(UUID id, long studentId, UUID courseId, String courseTitle, UUID sessionId,
                             String topic, Instant heldAt, boolean attended, String learned,
                             String participation, String recommendation, Instant publishedAt) {
        public static ReportView of(StudentLessonReport report) {
            var session = report.getSession();
            var enrollment = report.getEnrollment();
            var course = enrollment.getCourse();
            return new ReportView(report.getId(), enrollment.getStudentAccountId(), course.getId(), course.getTitle(),
                    session.getId(), session.getTopic(), session.getHeldAt(), report.isAttended(), report.getLearned(),
                    report.getParticipation(), report.getRecommendation(), report.getPublishedAt());
        }
    }
}
