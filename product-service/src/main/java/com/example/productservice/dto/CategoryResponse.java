package com.example.productservice.dto;

import com.example.productservice.model.Category;

public record CategoryResponse(String id, String name, String slug, String parentCategoryId) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getParentCategoryId());
    }
}
