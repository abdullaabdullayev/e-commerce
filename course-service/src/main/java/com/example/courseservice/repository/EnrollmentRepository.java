package com.example.courseservice.repository;

import com.example.courseservice.domain.Enrollment;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    Optional<Enrollment> findByCourseIdAndStudentAccountId(UUID courseId, long accountId);
    Slice<Enrollment> findByCourseIdOrderByEnrolledAtAscIdAsc(UUID courseId, Pageable pageable);

    @EntityGraph(attributePaths = "course")
    @Query("""
        select e from Enrollment e where e.studentAccountId = :accountId
        and e.course.status <> com.example.courseservice.domain.CourseStatus.DRAFT
        order by e.enrolledAt desc, e.id
        """)
    Slice<Enrollment> findLearning(long accountId, Pageable pageable);
}
