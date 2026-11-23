package com.example.productservice.repository;

import com.example.productservice.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Repository
@RequiredArgsConstructor
public class CatalogDocumentRepository {
    private final MongoTemplate mongo;
    public record Search(String text, String category, String brand, String currency,
                         BigDecimal minimum, BigDecimal maximum, boolean includeInactive) {}

    public Page<Product> search(Search filter, Pageable page) {
        Criteria criteria = new Criteria();
        if (!filter.includeInactive()) criteria.and("active").is(true);
        if (filter.text() != null) {
            String regex = Pattern.quote(filter.text());
            criteria.orOperator(Criteria.where("name").regex(regex, "i"), Criteria.where("sku").regex(regex, "i"));
        }
        if (filter.category() != null) criteria.and("categoryId").is(filter.category());
        if (filter.brand() != null) criteria.and("brandId").is(filter.brand());
        if (filter.currency() != null) criteria.and("currency").is(filter.currency());
        if (filter.minimum() != null || filter.maximum() != null) {
            Criteria price = criteria.and("price");
            if (filter.minimum() != null) price.gte(filter.minimum());
            if (filter.maximum() != null) price.lte(filter.maximum());
        }
        Query query = Query.query(criteria);
        long count = mongo.count(query, Product.class);
        return new PageImpl<>(mongo.find(query.with(page), Product.class), page, count);
    }

    public Optional<Product> updateDetails(String id, Product values) {
        Update update = new Update().set("name", values.getName()).set("description", values.getDescription())
                .set("categoryId", values.getCategoryId()).set("brandId", values.getBrandId())
                .set("price", values.getPrice()).set("currency", values.getCurrency())
                .set("images", values.getImages()).set("attributes", values.getAttributes());
        return updateProduct(id, update);
    }

    public Optional<Product> setActive(String id, boolean active) {
        return updateProduct(id, new Update().set("active", active));
    }

    private Optional<Product> updateProduct(String id, Update update) {
        update.set("updatedAt", LocalDateTime.now());
        return Optional.ofNullable(mongo.findAndModify(Query.query(Criteria.where("id").is(id)), update,
                FindAndModifyOptions.options().returnNew(true), Product.class));
    }

    public Optional<Category> updateCategory(String id, String name, String slug) {
        return Optional.ofNullable(mongo.findAndModify(Query.query(Criteria.where("id").is(id)),
                new Update().set("name", name).set("slug", slug), FindAndModifyOptions.options().returnNew(true), Category.class));
    }
}
