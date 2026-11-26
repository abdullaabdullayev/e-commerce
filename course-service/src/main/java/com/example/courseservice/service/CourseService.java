package com.example.courseservice.service;

import com.example.courseservice.security.Actor;
import com.example.courseservice.domain.*;
import com.example.courseservice.dto.*;
import com.example.courseservice.repository.CourseRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static com.example.courseservice.dto.CourseDtos.*;
import static com.example.courseservice.exception.DomainException.*;

@Service
@Transactional(readOnly = true)
public class CourseService {
    private final CourseRepository courses;
    private final CourseAccess access;

    public CourseService(CourseRepository courses, CourseAccess access) {
        this.courses = courses;
        this.access = access;
    }

    public PageResponse<CourseResponse> published(int page, int size) {
        return PageResponse.from(courses.findByStatusOrderByCreatedAtDescIdAsc(
                CourseStatus.PUBLISHED, PageRequest.of(page, size)).map(CourseMapper::course));
    }

    public CourseResponse published(UUID id) {
        Course course = access.course(id);
        if (course.getStatus() != CourseStatus.PUBLISHED) throw notFound("Course");
        return CourseMapper.course(course);
    }

    public PageResponse<CourseResponse> managed(Actor actor, int page, int size) {
        var pageable = PageRequest.of(page, size);
        if (actor.admin()) return PageResponse.from(courses.findAllByOrderByCreatedAtDescIdAsc(pageable).map(CourseMapper::course));
        if (!actor.teacher()) throw forbidden();
        return PageResponse.from(courses.findAssigned(actor.id(), pageable).map(CourseMapper::course));
    }

    public PageResponse<CourseResponse> adminList(Actor actor, String query, CourseStatus status, int page, int size) {
        actor.requireAdmin();
        var pageable = PageRequest.of(page, size);
        String title = query.strip();
        var result = status == null
                ? courses.findByTitleContainingIgnoreCaseOrderByCreatedAtDescIdAsc(title, pageable)
                : courses.findByStatusAndTitleContainingIgnoreCaseOrderByCreatedAtDescIdAsc(status, title, pageable);
        return PageResponse.from(result.map(CourseMapper::course));
    }

    @Transactional
    public CourseResponse create(Actor actor, CourseRequest request) {
        actor.requireAdmin();
        validateAges(request);
        Course course = new Course(request.title(), request.description(), request.level(), request.minAge(), request.maxAge());
        return CourseMapper.course(courses.save(course));
    }

    @Transactional
    public CourseResponse update(Actor actor, UUID id, CourseRequest request) {
        Course course = access.lockAdminEditable(actor, id);
        validateAges(request);
        course.revise(request.title(), request.description(), request.level(), request.minAge(), request.maxAge());
        return CourseMapper.course(course);
    }

    @Transactional
    public CourseResponse changeStatus(Actor actor, UUID id, CourseStatus status) {
        // Unlike content edits, a status change may also restore an archived course.
        actor.requireAdmin();
        Course course = access.lock(id);
        course.changeStatus(status);
        return CourseMapper.course(course);
    }

    private void validateAges(CourseRequest request) {
        if (request.minAge() > request.maxAge()) throw badRequest("Minimum age exceeds maximum age");
    }
}
