package com.example.productservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collation = "products")
public class Product {
    @Id
    private String id;
    private String name;
    private String description;
    private String sku;
    private String categoryId;
    private String brandId;
    private List<String> images;

    private Map<String, Object> attributes;

    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
