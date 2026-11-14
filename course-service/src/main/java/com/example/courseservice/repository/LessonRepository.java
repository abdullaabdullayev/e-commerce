package com.example.courseservice.repository;

import com.example.courseservice.domain.Lesson;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByCourseIdOrderByPosition(UUID courseId);
    Optional<Lesson> findByIdAndCourseId(UUID id, UUID courseId);
    long countByCourseId(UUID courseId);

    @Query("select l.course.id from Lesson l where l.id = :id")
    Optional<UUID> findCourseId(UUID id);

    // The existing PostgreSQL constraint supports atomic position swaps.
    @Modifying
    @Query(value = "SET CONSTRAINTS lessons_course_position DEFERRED", nativeQuery = true)
    void deferPositionConstraint();
}
