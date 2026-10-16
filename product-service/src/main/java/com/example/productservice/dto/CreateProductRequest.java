package com.example.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CreateProductRequest(
        @NotBlank String name,
        String description,
        @NotBlank String sku,
        @NotBlank String categoryId,
        String brandId,
        @NotNull @Positive BigDecimal price,
        @NotBlank @Size(min = 3, max = 3) String currency,
        List<String> images,
        Map<String, Object> attributes
        ) {}