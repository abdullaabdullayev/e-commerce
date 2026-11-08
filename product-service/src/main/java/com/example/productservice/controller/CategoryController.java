package com.example.productservice.controller;

import com.example.productservice.dto.CategoryRequests;
import com.example.productservice.model.Category;
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
    public Page<Category> list(@PageableDefault(size = 20) Pageable pageable) { return service.list(pageable); }

    @GetMapping("/{id}")
    public Category get(@PathVariable String id) { return service.require(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category create(@Valid @RequestBody CategoryRequests.Create request) { return service.create(request); }

    @PutMapping("/{id}")
    public Category update(@PathVariable String id, @Valid @RequestBody CategoryRequests.Update request) {
        return service.update(id, request);
    }
}
