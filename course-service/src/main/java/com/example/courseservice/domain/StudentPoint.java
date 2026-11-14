package com.example.courseservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_points")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentPoint {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false, updatable = false)
    private Enrollment enrollment;
    @Column(nullable = false, updatable = false)
    private UUID requestId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PointType type;
    @Column(nullable = false, updatable = false)
    private int points;
    @Column(nullable = false, updatable = false)
    private int requestedPoints;
    @Column(nullable = false, updatable = false, length = 500)
    private String reason;
    @Column(nullable = false, updatable = false)
    private long awardedByAccountId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_award_id", updatable = false)
    private StudentPoint originalAward;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public StudentPoint(Enrollment enrollment, UUID requestId, PointType type, int points,
                        int requestedPoints, String reason, long actorId, StudentPoint originalAward) {
        id = UUID.randomUUID();
        this.enrollment = enrollment;
        this.requestId = requestId;
        this.type = type;
        this.points = points;
        this.requestedPoints = requestedPoints;
        this.reason = reason.trim();
        this.awardedByAccountId = actorId;
        this.originalAward = originalAward;
        createdAt = Instant.now();
    }
}
