package com.example.courseservice.repository;

import com.example.courseservice.domain.StudentLessonReport;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface StudentLessonReportRepository extends JpaRepository<StudentLessonReport, UUID> {
    Optional<StudentLessonReport> findBySessionIdAndEnrollmentId(UUID sessionId, UUID enrollmentId);
    Optional<StudentLessonReport> findBySessionIdAndEnrollmentStudentAccountId(UUID sessionId, long studentId);

    @EntityGraph(attributePaths = {"enrollment.course", "session"})
    List<StudentLessonReport> findBySessionIdAndEnrollmentStudentAccountIdIn(UUID sessionId, Set<Long> studentIds);

    @EntityGraph(attributePaths = {"enrollment.course", "session"})
    Slice<StudentLessonReport> findBySessionIdOrderByEnrollmentStudentAccountId(UUID sessionId, Pageable page);

    @EntityGraph(attributePaths = {"enrollment.course", "session"})
    @Query("""
        select r from StudentLessonReport r where r.enrollment.studentAccountId = :studentId
        and r.publishedAt is not null and r.enrollment.course.status <> com.example.courseservice.domain.CourseStatus.DRAFT
        order by r.session.heldAt desc, r.id
        """)
    Slice<StudentLessonReport> publishedForChild(long studentId, Pageable page);
}
