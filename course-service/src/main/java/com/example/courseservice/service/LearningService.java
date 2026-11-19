package com.example.courseservice.service;

import com.example.courseservice.security.Actor;
import com.example.courseservice.dto.*;
import com.example.courseservice.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import static com.example.courseservice.dto.LearningDtos.*;
import static com.example.courseservice.exception.DomainException.*;

@Service
@Transactional(readOnly = true)
public class LearningService {
    private final EnrollmentRepository enrollments;
    private final StudentPointRepository points;
    private final CourseTeacherRepository teachers;

    public LearningService(EnrollmentRepository enrollments, StudentPointRepository points, CourseTeacherRepository teachers) {
        this.enrollments = enrollments;
        this.points = points;
        this.teachers = teachers;
    }

    public PageResponse<LearningResponse> mine(Actor actor, int page, int size) {
        if (!actor.student()) throw forbidden();
        return forStudent(actor.id(), page, size);
    }

    public PageResponse<LearningResponse> forStudent(long studentId, int page, int size) {
        var slice = enrollments.findLearning(studentId, PageRequest.of(page, size));
        if (slice.isEmpty()) return PageResponse.from(slice.map(e -> new LearningResponse(
                CourseMapper.course(e.getCourse()), e.getStatus(), List.of(), 0)));
        var ids = slice.getContent().stream().map(e -> e.getId()).toList();
        var scores = points.totals(ids).stream().collect(Collectors.toMap(
                StudentPointRepository.ScoreTotal::getEnrollmentId, StudentPointRepository.ScoreTotal::getTotal));
        var courseIds = slice.getContent().stream().map(e -> e.getCourse().getId()).toList();
        var teacherIds = teachers.findByIdCourseIdInAndActiveTrue(courseIds).stream().collect(
                Collectors.groupingBy(t -> t.getId().getCourseId(),
                        Collectors.mapping(t -> t.getId().getAccountId(), Collectors.toList())));
        return PageResponse.from(slice.map(e -> new LearningResponse(CourseMapper.course(e.getCourse()),
                e.getStatus(), teacherIds.getOrDefault(e.getCourse().getId(), List.of()), scores.getOrDefault(e.getId(), 0L))));
    }
}
