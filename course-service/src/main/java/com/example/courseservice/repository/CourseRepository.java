package com.example.courseservice.repository;

import com.example.courseservice.domain.Course;
import com.example.courseservice.domain.CourseStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id = :id")
    Optional<Course> findForUpdate(UUID id);

    Slice<Course> findByStatusOrderByCreatedAtDescIdAsc(CourseStatus status, Pageable pageable);
    Slice<Course> findAllByOrderByCreatedAtDescIdAsc(Pageable pageable);

    @Query("""
        select c from Course c join CourseTeacher t on t.course = c
        where t.id.accountId = :accountId and t.active = true
        order by c.createdAt desc, c.id
        """)
    Slice<Course> findAssigned(long accountId, Pageable pageable);
}
