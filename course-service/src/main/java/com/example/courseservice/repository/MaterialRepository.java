package com.example.courseservice.repository;

import com.example.courseservice.domain.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import java.util.*;

public interface MaterialRepository extends JpaRepository<Material, UUID> {
    List<Material> findByLessonCourseIdAndStateOrderByCreatedAtAscIdAsc(UUID courseId, MaterialState state);
    long countByLessonIdAndStateIn(UUID lessonId, Collection<MaterialState> states);

    @Query("select m.lesson.course.id from Material m where m.id = :id")
    Optional<UUID> findCourseId(UUID id);

    @Query("""
        select m.id from Material m where m.kind = 'FILE' and
        (m.state = com.example.courseservice.domain.MaterialState.FAILED
        or (m.state = com.example.courseservice.domain.MaterialState.PENDING and m.createdAt < :pendingBefore)
        or (m.state = com.example.courseservice.domain.MaterialState.CLEANING and m.cleanupStartedAt < :cleanupBefore))
        order by m.createdAt, m.id
        """)
    List<UUID> findCleanupCandidates(Instant pendingBefore, Instant cleanupBefore, Pageable pageable);
}
