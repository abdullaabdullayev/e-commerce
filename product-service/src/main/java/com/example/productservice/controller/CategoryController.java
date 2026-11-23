package com.example.productservice.controller;

import com.example.productservice.dto.CategoryRequests;
import com.example.productservice.dto.CategoryResponse;
import com.example.productservice.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;

    @GetMapping
    public Page<CategoryResponse> list(@PageableDefault(size = 20) Pageable pageable) { return service.list(pageable).map(CategoryResponse::from); }

    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable String id) { return CategoryResponse.from(service.require(id)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequests.Create request) { return CategoryResponse.from(service.create(request)); }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable String id, @Valid @RequestBody CategoryRequests.Update request) {
        return CategoryResponse.from(service.update(id, request));
    }
}
