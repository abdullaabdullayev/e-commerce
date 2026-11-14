package com.example.courseservice.dto;

import com.example.courseservice.domain.EnrollmentStatus;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class LearningDtos {
    private LearningDtos() {}

    public record EnrollmentRequest(@NotNull EnrollmentStatus status) {}
    public record EnrollmentResponse(UUID id, UUID courseId, long studentAccountId,
                                     EnrollmentStatus status, Instant enrolledAt) {}
    public record Profile(long accountId, String firstName, String lastName, String guardianName, String guardianContact) {}
    public record RosterEntry(EnrollmentResponse enrollment, Profile profile, boolean profilePending) {}
    public record RosterResponse(List<RosterEntry> content, int page, boolean hasNext, boolean profilesUnavailable) {}
    public record LearningResponse(CourseDtos.CourseResponse course, EnrollmentStatus enrollmentStatus,
                                   List<Long> teacherAccountIds, long totalPoints) {}
}
