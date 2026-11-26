package com.example.courseservice.service;

import com.example.courseservice.domain.*;
import com.example.courseservice.dto.PageResponse;
import com.example.courseservice.repository.*;
import com.example.courseservice.security.Actor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.example.courseservice.exception.DomainException.*;
import static com.example.courseservice.dto.ReportDtos.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonReportService {
    private final CourseAccess access;
    private final LessonRepository lessons;
    private final EnrollmentRepository enrollments;
    private final LessonSessionRepository sessions;
    private final StudentLessonReportRepository reports;

    @Transactional
    public SessionView recordSession(Actor actor, UUID courseId, NewSession request) {
        // Authorize before taking the course lock so outsiders cannot hold it.
        access.requireStaff(actor, courseId);
        Course course = access.lock(courseId);
        if (course.getStatus() != CourseStatus.PUBLISHED) throw conflict("Publish the course before recording a session");
        var lesson = lessons.findByIdAndCourseId(request.lessonId(), courseId).orElseThrow(() -> notFound("Lesson"));
        if (lesson.isArchived()) throw conflict("Lesson is archived");
        Instant heldAt = request.heldAt().truncatedTo(ChronoUnit.MICROS);
        var session = sessions.findByLessonIdAndTeacherAccountIdAndHeldAt(lesson.getId(), actor.id(), heldAt)
                .orElseGet(() -> sessions.save(new LessonSession(lesson, actor.id(), heldAt)));
        return SessionView.of(session);
    }

    public PageResponse<SessionView> sessions(Actor actor, UUID courseId, Pageable page) {
        access.requireStaff(actor, courseId);
        var visible = actor.admin() ? sessions.findByLessonCourseIdOrderByHeldAtDescIdAsc(courseId, page)
                : sessions.findByLessonCourseIdAndTeacherAccountIdOrderByHeldAtDescIdAsc(courseId, actor.id(), page);
        return PageResponse.from(visible.map(SessionView::of));
    }

    public PageResponse<ReportView> drafts(Actor actor, UUID courseId, UUID sessionId, Pageable page) {
        requireAuthor(actor, courseId, sessionId);
        return PageResponse.from(reports.findBySessionIdOrderByEnrollmentStudentAccountId(sessionId, page)
                .map(ReportView::of));
    }

    public Optional<ReportView> studentDraft(Actor actor, UUID courseId, UUID sessionId, long studentId) {
        requireAuthor(actor, courseId, sessionId);
        return reports.findBySessionIdAndEnrollmentStudentAccountId(sessionId, studentId).map(ReportView::of);
    }

    @Transactional
    public ReportView saveDraft(Actor actor, UUID courseId, UUID sessionId, long studentId, Draft draft) {
        var session = editableSession(actor, courseId, sessionId);
        var enrollment = enrollments.findByCourseIdAndStudentAccountId(courseId, studentId)
                .orElseThrow(() -> notFound("Enrollment"));
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE || enrollment.getEnrolledAt().isAfter(session.getHeldAt())) {
            throw conflict("The student needs an active enrollment covering this lesson");
        }
        var report = reports.findBySessionIdAndEnrollmentId(sessionId, enrollment.getId())
                .orElseGet(() -> new StudentLessonReport(session, enrollment));
        report.revise(draft.attended(), draft.learned(), draft.participation(), draft.recommendation());
        return ReportView.of(reports.save(report));
    }

    @Transactional
    public void publish(Actor actor, UUID courseId, UUID sessionId, Publish request) {
        editableSession(actor, courseId, sessionId);
        var ids = new HashSet<>(request.studentIds());
        var selected = reports.findBySessionIdAndEnrollmentStudentAccountIdIn(sessionId, ids);
        if (selected.size() != ids.size()) throw conflict("Each selected student needs a saved report");
        Instant now = Instant.now();
        selected.forEach(report -> report.publish(now));
    }

    public PageResponse<ReportView> publishedForChild(long studentId, Pageable page) {
        return PageResponse.from(reports.publishedForChild(studentId, page).map(ReportView::of));
    }

    private LessonSession editableSession(Actor actor, UUID courseId, UUID sessionId) {
        var session = requireAuthor(actor, courseId, sessionId);
        Course course = access.lock(courseId);
        if (course.getStatus() != CourseStatus.PUBLISHED) throw conflict("Course is not published");
        return session;
    }

    private LessonSession requireAuthor(Actor actor, UUID courseId, UUID sessionId) {
        access.requireStaff(actor, courseId);
        var session = sessions.findByIdAndLessonCourseId(sessionId, courseId).orElseThrow(() -> notFound("Session"));
        if (!actor.admin() && session.getTeacherAccountId() != actor.id()) throw forbidden();
        return session;
    }
}
