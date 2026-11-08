package com.example.productservice.dto;

import jakarta.validation.constraints.*;

public final class CategoryRequests {
    private CategoryRequests() {}

    public record Create(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max = 100) String slug,
            @Size(max = 100) String parentCategoryId
    ) {}

    // Parent is immutable: new categories may reference only existing parents.
    public record Update(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max = 100) String slug
    ) {}
}
