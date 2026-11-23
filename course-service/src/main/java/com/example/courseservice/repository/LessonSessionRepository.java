package com.example.courseservice.repository;

import com.example.courseservice.domain.LessonSession;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.*;

public interface LessonSessionRepository extends JpaRepository<LessonSession, UUID> {
    Optional<LessonSession> findByIdAndLessonCourseId(UUID id, UUID courseId);
    Optional<LessonSession> findByLessonIdAndTeacherAccountIdAndHeldAt(UUID lessonId, long teacherId, Instant heldAt);
    Slice<LessonSession> findByLessonCourseIdOrderByHeldAtDescIdAsc(UUID courseId, Pageable page);
    Slice<LessonSession> findByLessonCourseIdAndTeacherAccountIdOrderByHeldAtDescIdAsc(UUID courseId, long teacherId, Pageable page);
}
