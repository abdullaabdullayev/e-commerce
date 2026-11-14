package com.example.courseservice.repository;

import com.example.courseservice.domain.StudentPoint;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface StudentPointRepository extends JpaRepository<StudentPoint, UUID> {
    Optional<StudentPoint> findByEnrollmentIdAndRequestId(UUID enrollmentId, UUID requestId);
    Optional<StudentPoint> findByIdAndEnrollmentId(UUID id, UUID enrollmentId);
    Slice<StudentPoint> findByEnrollmentIdOrderByCreatedAtDescIdAsc(UUID enrollmentId, Pageable pageable);

    @Query("select coalesce(sum(p.points), 0) from StudentPoint p where p.enrollment.id = :enrollmentId")
    long total(UUID enrollmentId);

    @Query("select coalesce(sum(p.points), 0) from StudentPoint p left join p.originalAward original where p.id = :awardId or original.id = :awardId")
    long effectiveAward(UUID awardId);

    interface ScoreTotal {
        UUID getEnrollmentId();
        Long getTotal();
    }

    @Query("""
        select p.enrollment.id as enrollmentId, sum(p.points) as total from StudentPoint p
        where p.enrollment.id in :enrollmentIds group by p.enrollment.id
        """)
    List<ScoreTotal> totals(Collection<UUID> enrollmentIds);
}
