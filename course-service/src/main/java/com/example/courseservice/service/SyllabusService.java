package com.example.courseservice.service;

import com.example.courseservice.security.Actor;
import com.example.courseservice.domain.*;
import com.example.courseservice.dto.CourseMapper;
import com.example.courseservice.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import static com.example.courseservice.dto.CourseDtos.*;
import static com.example.courseservice.exception.DomainException.*;

@Service
@Transactional(readOnly = true)
public class SyllabusService {
    private final CourseAccess access;
    private final LessonRepository lessons;
    private final MaterialRepository materials;
    private final CourseTeacherRepository teachers;

    public SyllabusService(CourseAccess access, LessonRepository lessons, MaterialRepository materials,
                           CourseTeacherRepository teachers) {
        this.access = access;
        this.lessons = lessons;
        this.materials = materials;
        this.teachers = teachers;
    }

    public SyllabusResponse read(Actor actor, UUID courseId) {
        access.requireStaff(actor, courseId);
        Course course = access.course(courseId);
        var byLesson = materials.findByLessonCourseIdAndStateOrderByCreatedAtAscIdAsc(courseId, MaterialState.READY)
                .stream().collect(Collectors.groupingBy(m -> m.getLesson().getId(),
                        Collectors.mapping(CourseMapper::material, Collectors.toList())));
        var content = lessons.findByCourseIdOrderByPosition(courseId).stream()
                .map(lesson -> new LessonContent(CourseMapper.lesson(lesson),
                        byLesson.getOrDefault(lesson.getId(), List.of()))).toList();
        var teacherIds = teachers.findByIdCourseIdAndActiveTrueOrderByIdAccountId(courseId)
                .stream().map(t -> t.getId().getAccountId()).toList();
        return new SyllabusResponse(CourseMapper.course(course), teacherIds, content);
    }

    @Transactional
    public LessonResponse create(Actor actor, UUID courseId, LessonRequest request) {
        Course course = access.lockAdminEditable(actor, courseId);
        long count = lessons.countByCourseId(courseId);
        if (count >= 500) throw conflict("A course supports up to 500 lessons");
        Lesson lesson = new Lesson(course, (int) count + 1, request.title(), request.objective(),
                request.durationMinutes(), request.teacherNotes());
        return CourseMapper.lesson(lessons.save(lesson));
    }

    @Transactional
    public LessonResponse update(Actor actor, UUID courseId, UUID lessonId, LessonRequest request) {
        access.lockAdminEditable(actor, courseId);
        Lesson lesson = requireLesson(courseId, lessonId);
        if (lesson.isArchived()) throw conflict("Lesson is archived");
        lesson.revise(request.title(), request.objective(), request.durationMinutes(), request.teacherNotes());
        return CourseMapper.lesson(lesson);
    }

    @Transactional
    public void reorder(Actor actor, UUID courseId, List<UUID> orderedIds) {
        access.lockAdminEditable(actor, courseId);
        var existing = lessons.findByCourseIdOrderByPosition(courseId);
        var byId = existing.stream().collect(Collectors.toMap(Lesson::getId, lesson -> lesson));
        if (orderedIds.size() != new HashSet<>(orderedIds).size()
                || !byId.keySet().equals(new HashSet<>(orderedIds))) {
            throw badRequest("Supply every lesson exactly once, including archived lessons");
        }
        lessons.deferPositionConstraint();
        for (int i = 0; i < orderedIds.size(); i++) byId.get(orderedIds.get(i)).reorder(i + 1);
        lessons.flush();
    }

    @Transactional
    public void archive(Actor actor, UUID courseId, UUID lessonId) {
        access.lockAdminEditable(actor, courseId);
        requireLesson(courseId, lessonId).archive();
    }

    private Lesson requireLesson(UUID courseId, UUID lessonId) {
        return lessons.findByIdAndCourseId(lessonId, courseId).orElseThrow(() -> notFound("Lesson"));
    }
}
