package com.example.courseservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_enrollments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, updatable = false)
    private Course course;
    @Column(nullable = false, updatable = false)
    private long studentAccountId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;
    @Column(nullable = false, updatable = false)
    private Instant enrolledAt;

    public Enrollment(Course course, long studentAccountId) {
        id = UUID.randomUUID();
        this.course = course;
        this.studentAccountId = studentAccountId;
        status = EnrollmentStatus.ACTIVE;
        enrolledAt = Instant.now();
    }

    public void changeStatus(EnrollmentStatus status) {
        this.status = status;
    }
}
