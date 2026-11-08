package com.example.productservice.service;

import com.example.productservice.dto.CategoryRequests;
import com.example.productservice.model.Category;
import com.example.productservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository repository;
    private final MongoTemplate mongo;

    public Category require(String id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    public Page<Category> list(Pageable pageable) {
        return repository.findAll(PageRequest.of(pageable.getPageNumber(), Math.min(100, pageable.getPageSize()),
                Sort.by("name").ascending().and(Sort.by("id"))));
    }

    public Category create(CategoryRequests.Create request) {
        String parent = CatalogRules.optionalText(request.parentCategoryId());
        if (parent != null) require(parent);
        try {
            return repository.insert(Category.builder().name(request.name().trim()).slug(request.slug())
                    .parentCategoryId(parent).build());
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category slug already exists");
        }
    }

    public Category update(String id, CategoryRequests.Update request) {
        try {
            Category category = mongo.findAndModify(Query.query(Criteria.where("id").is(id)),
                    new Update().set("name", request.name().trim()).set("slug", request.slug()),
                    FindAndModifyOptions.options().returnNew(true), Category.class);
            if (category == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
            return category;
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category slug already exists");
        }
    }
}
