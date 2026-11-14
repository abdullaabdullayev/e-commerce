package com.example.courseservice.service;

import com.example.courseservice.security.Actor;
import com.example.courseservice.domain.*;
import com.example.courseservice.repository.*;
import org.springframework.stereotype.Component;
import java.util.UUID;
import static com.example.courseservice.exception.DomainException.*;

@Component
public class CourseAccess {
    private final CourseRepository courses;
    private final CourseTeacherRepository teachers;

    public CourseAccess(CourseRepository courses, CourseTeacherRepository teachers) {
        this.courses = courses;
        this.teachers = teachers;
    }

    public Course course(UUID id) {
        return courses.findById(id).orElseThrow(() -> notFound("Course"));
    }

    // Every course mutation uses this lock, including membership and point writes.
    // Call only inside the caller's transaction.
    public Course lock(UUID id) {
        return courses.findForUpdate(id).orElseThrow(() -> notFound("Course"));
    }

    public void requireStaff(Actor actor, UUID courseId) {
        if (actor.admin()) return;
        if (!actor.teacher() || !teachers.existsByIdAndActiveTrue(new CourseTeacherId(courseId, actor.id()))) {
            throw forbidden();
        }
    }

    public Course lockAdminEditable(Actor actor, UUID courseId) {
        actor.requireAdmin();
        Course course = lock(courseId);
        requireEditable(course);
        return course;
    }

    public void requireEditable(Course course) {
        if (course.getStatus() == CourseStatus.ARCHIVED) throw conflict("Course is archived");
    }
}
