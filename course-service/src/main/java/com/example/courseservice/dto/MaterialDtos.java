package com.example.courseservice.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public final class MaterialDtos {
    private MaterialDtos() {}

    public record LinkRequest(@NotBlank @Size(max = 160) String title, @NotBlank @Size(max = 2048) String url) {}
    public record MaterialResponse(UUID id, String title, String kind, String filename,
                                   String contentType, Long sizeBytes, String url) {}
}
