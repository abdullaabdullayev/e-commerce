package com.example.productservice.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Full replacement of editable details. SKU and active status have separate semantics. */
public record UpdateProductRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 10000) String description,
        @NotBlank @Size(max = 100) String categoryId,
        @Size(max = 100) String brandId,
        @NotNull @Positive BigDecimal price,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @Size(max = 20) List<@NotBlank @Size(max = 2048) String> images,
        @Size(max = 50) Map<String, Object> attributes
) {}
