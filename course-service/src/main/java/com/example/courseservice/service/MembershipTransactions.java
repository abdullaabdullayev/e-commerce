package com.example.courseservice.service;

import com.example.courseservice.security.Actor;
import com.example.courseservice.domain.*;
import com.example.courseservice.dto.*;
import com.example.courseservice.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static com.example.courseservice.dto.LearningDtos.*;
import static com.example.courseservice.exception.DomainException.*;

@Service
@Transactional(readOnly = true)
public class MembershipTransactions {
    private final CourseAccess access;
    private final CourseTeacherRepository teachers;
    private final EnrollmentRepository enrollments;

    public MembershipTransactions(CourseAccess access, CourseTeacherRepository teachers, EnrollmentRepository enrollments) {
        this.access = access;
        this.teachers = teachers;
        this.enrollments = enrollments;
    }

    @Transactional
    public void assignTeacher(Actor actor, UUID courseId, long accountId) {
        Course course = access.lockAdminEditable(actor, courseId);
        CourseTeacherId id = new CourseTeacherId(courseId, accountId);
        teachers.findById(id).ifPresentOrElse(CourseTeacher::activate,
                () -> teachers.save(new CourseTeacher(course, accountId)));
    }

    @Transactional
    public void removeTeacher(Actor actor, UUID courseId, long accountId) {
        actor.requireAdmin();
        access.lock(courseId);
        teachers.findById(new CourseTeacherId(courseId, accountId)).ifPresent(CourseTeacher::deactivate);
    }

    @Transactional
    public EnrollmentResponse enroll(Actor actor, UUID courseId, long accountId, EnrollmentStatus status) {
        actor.requireAdmin();
        Course course = access.lock(courseId);
        var existing = enrollments.findByCourseIdAndStudentAccountId(courseId, accountId);
        if (status == EnrollmentStatus.ACTIVE) access.requireEditable(course);
        if (existing.isEmpty() && status != EnrollmentStatus.ACTIVE) throw notFound("Enrollment");
        Enrollment enrollment = existing.orElseGet(() -> enrollments.save(new Enrollment(course, accountId)));
        enrollment.changeStatus(status);
        return CourseMapper.enrollment(enrollment);
    }

    public PageResponse<EnrollmentResponse> roster(Actor actor, UUID courseId, int page) {
        access.requireStaff(actor, courseId);
        access.course(courseId);
        return PageResponse.from(enrollments.findByCourseIdOrderByEnrolledAtAscIdAsc(courseId,
                PageRequest.of(page, 50)).map(CourseMapper::enrollment));
    }
}
