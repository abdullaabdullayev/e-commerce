package com.example.productservice.controller;

import com.example.productservice.dto.ProductResponse;
import com.example.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/catalog/products")
@RequiredArgsConstructor
public class CatalogAdministrationController {
    private final ProductService service;

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable String id) { return service.findForAdministration(id); }

    @GetMapping
    public Page<ProductResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.search(null, null, null, null, null, null, pageable, true);
    }
}
