package com.example.productservice.dto;

import com.example.productservice.model.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ProductResponse(

        String id,
        String name,
        String description,
        String sku,
        String categoryId,
        String brandId,
        BigDecimal price,
        String currency,
        List<String> images,
        Map<String, Object> attributes,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getSku(),
                p.getCategoryId(), p.getBrandId(), p.getPrice(), p.getCurrency(),
                p.getImages(), p.getAttributes(), p.isActive(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
