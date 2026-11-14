package com.example.courseservice.dto;

import com.example.courseservice.domain.PointType;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class PointDtos {
    private PointDtos() {}

    public record AwardRequest(@NotNull UUID requestId, @Min(1) @Max(1000) int points,
                               @NotBlank @Size(max = 500) String reason) {}
    public record CorrectionRequest(@NotNull UUID requestId, @NotNull @Min(0) @Max(1000) Integer correctedPoints,
                                    @NotBlank @Size(max = 500) String reason) {}
    public record PointResponse(UUID id, UUID requestId, PointType type, int points, int requestedPoints,
                                String reason, long awardedByAccountId, UUID originalAwardId, Instant createdAt) {}
    public record PointHistory(UUID courseId, long studentAccountId, long totalPoints,
                               PageResponse<PointResponse> entries) {}
}
