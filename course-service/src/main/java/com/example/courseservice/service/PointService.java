package com.example.courseservice.service;

import com.example.courseservice.security.Actor;
import com.example.courseservice.domain.*;
import com.example.courseservice.dto.*;
import com.example.courseservice.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;
import static com.example.courseservice.dto.PointDtos.*;
import static com.example.courseservice.exception.DomainException.*;

@Service
public class PointService {
    private final CourseAccess access;
    private final EnrollmentRepository enrollments;
    private final StudentPointRepository points;

    public PointService(CourseAccess access, EnrollmentRepository enrollments, StudentPointRepository points) {
        this.access = access;
        this.enrollments = enrollments;
        this.points = points;
    }

    @Transactional
    public PointResponse award(Actor actor, UUID courseId, long studentId, AwardRequest request) {
        Course course = access.lock(courseId);
        access.requireStaff(actor, courseId);
        Enrollment enrollment = requireEnrollment(courseId, studentId);
        var previous = points.findByEnrollmentIdAndRequestId(enrollment.getId(), request.requestId());
        if (previous.isPresent()) return replay(previous.get(), actor, PointType.AWARD,
                request.points(), request.reason(), null);
        if (course.getStatus() != CourseStatus.PUBLISHED || enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw conflict("Points require an active enrollment in a published course");
        }
        return CourseMapper.point(points.save(new StudentPoint(enrollment, request.requestId(), PointType.AWARD,
                request.points(), request.points(), request.reason(), actor.id(), null)));
    }

    @Transactional
    public PointResponse correct(Actor actor, UUID courseId, long studentId, UUID awardId, CorrectionRequest request) {
        actor.requireAdmin();
        access.lock(courseId);
        Enrollment enrollment = requireEnrollment(courseId, studentId);
        var previous = points.findByEnrollmentIdAndRequestId(enrollment.getId(), request.requestId());
        if (previous.isPresent()) return replay(previous.get(), actor, PointType.CORRECTION,
                request.correctedPoints(), request.reason(), awardId);
        StudentPoint original = points.findByIdAndEnrollmentId(awardId, enrollment.getId())
                .orElseThrow(() -> notFound("Point award"));
        if (original.getType() != PointType.AWARD) throw badRequest("Correct the original award, not a correction entry");
        long current = points.effectiveAward(awardId);
        int difference = Math.toIntExact(request.correctedPoints() - current);
        if (difference == 0) throw conflict("The award already has the requested value");
        return CourseMapper.point(points.save(new StudentPoint(enrollment, request.requestId(), PointType.CORRECTION,
                difference, request.correctedPoints(), request.reason(), actor.id(), original)));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PointHistory history(Actor actor, UUID courseId, long studentId, int page, int size) {
        boolean ownStudentHistory = actor.student() && actor.id() == studentId;
        if (!ownStudentHistory) access.requireStaff(actor, courseId);
        Enrollment enrollment = requireEnrollment(courseId, studentId);
        if (ownStudentHistory && !actor.admin() && enrollment.getCourse().getStatus() == CourseStatus.DRAFT) {
            throw forbidden();
        }
        return history(enrollment, page, size);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PointHistory familyHistory(UUID courseId, long studentId, int page, int size) {
        Enrollment enrollment = requireEnrollment(courseId, studentId);
        if (enrollment.getCourse().getStatus() == CourseStatus.DRAFT) throw notFound("Course");
        return history(enrollment, page, size);
    }

    private PointHistory history(Enrollment enrollment, int page, int size) {
        var entries = points.findByEnrollmentIdOrderByCreatedAtDescIdAsc(enrollment.getId(), PageRequest.of(page, size));
        return new PointHistory(enrollment.getCourse().getId(), enrollment.getStudentAccountId(), points.total(enrollment.getId()),
                PageResponse.from(entries.map(CourseMapper::point)));
    }

    private Enrollment requireEnrollment(UUID courseId, long studentId) {
        return enrollments.findByCourseIdAndStudentAccountId(courseId, studentId)
                .orElseThrow(() -> notFound("Enrollment"));
    }

    private PointResponse replay(StudentPoint existing, Actor actor, PointType type, int requested,
                                 String reason, UUID originalId) {
        UUID existingOriginal = existing.getOriginalAward() == null ? null : existing.getOriginalAward().getId();
        if (existing.getAwardedByAccountId() != actor.id() || existing.getType() != type
                || existing.getRequestedPoints() != requested || !existing.getReason().equals(reason.trim())
                || !Objects.equals(existingOriginal, originalId)) {
            throw conflict("requestId was already used for a different operation");
        }
        return CourseMapper.point(existing);
    }
}
