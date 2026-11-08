package com.example.productservice.service;

import com.example.productservice.dto.*;
import com.example.productservice.model.Product;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductService {
    private static final Set<String> SORT_FIELDS = Set.of("name", "sku", "price", "createdAt", "updatedAt", "id");
    private final ProductRepository productRepository;
    private final CategoryService categories;
    private final MongoTemplate mongo;

    public ProductResponse create(CreateProductRequest request) {
        String sku = request.sku().trim();
        if (!sku.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,79}")) throw CatalogRules.invalid("Invalid SKU format");
        String categoryId = request.categoryId().trim();
        categories.require(categoryId);
        String currency = CatalogRules.currency(request.currency());
        Product product = Product.builder()
                .name(request.name().trim()).description(CatalogRules.optionalText(request.description()))
                .sku(sku).categoryId(categoryId).brandId(CatalogRules.optionalText(request.brandId()))
                .price(CatalogRules.price(request.price(), currency)).currency(currency)
                .images(CatalogRules.images(request.images())).attributes(CatalogRules.attributes(request.attributes()))
                .active(true).build();
        try {
            return ProductResponse.from(productRepository.insert(product));
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists");
        }
    }

    public ProductResponse update(String id, UpdateProductRequest request) {
        String categoryId = request.categoryId().trim();
        categories.require(categoryId);
        String currency = CatalogRules.currency(request.currency());
        // Atomic update preserves immutable SKU, creation date and active status.
        Update update = new Update().set("name", request.name().trim())
                .set("description", CatalogRules.optionalText(request.description()))
                .set("categoryId", categoryId).set("brandId", CatalogRules.optionalText(request.brandId()))
                .set("price", CatalogRules.price(request.price(), currency)).set("currency", currency)
                .set("images", CatalogRules.images(request.images())).set("attributes", CatalogRules.attributes(request.attributes()))
                .set("updatedAt", LocalDateTime.now());
        return modify(id, update);
    }

    public ProductResponse setActive(String id, boolean active) {
        return modify(id, new Update().set("active", active).set("updatedAt", LocalDateTime.now()));
    }

    private ProductResponse modify(String id, Update update) {
        Product result = mongo.findAndModify(Query.query(Criteria.where("id").is(id)), update,
                FindAndModifyOptions.options().returnNew(true), Product.class);
        if (result == null) throw notFound();
        return ProductResponse.from(result);
    }

    public ProductResponse findById(String id) {
        return productRepository.findById(id).filter(Product::isActive)
                .map(ProductResponse::from).orElseThrow(ProductService::notFound);
    }

    public ProductResponse findForAdministration(String id) {
        return productRepository.findById(id).map(ProductResponse::from).orElseThrow(ProductService::notFound);
    }

    public Page<ProductResponse> search(String text, String categoryId, String brandId, String currency,
                                        BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable,
                                        boolean includeInactive) {
        String code = CatalogRules.optionalText(currency);
        if (code != null) code = CatalogRules.currency(code);
        if ((minPrice != null || maxPrice != null) && code == null) {
            throw CatalogRules.invalid("Currency is required when filtering by price");
        }
        validateBound(minPrice);
        validateBound(maxPrice);
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw CatalogRules.invalid("minPrice must not exceed maxPrice");
        }
        Criteria criteria = new Criteria();
        if (!includeInactive) criteria.and("active").is(true);
        String term = CatalogRules.optionalText(text);
        if (term != null) {
            if (term.length() > 100) throw CatalogRules.invalid("Search term is too long");
            String regex = Pattern.quote(term);
            criteria.orOperator(Criteria.where("name").regex(regex, "i"), Criteria.where("sku").regex(regex, "i"));
        }
        String category = CatalogRules.optionalText(categoryId);
        if (category != null) criteria.and("categoryId").is(category);
        String brand = CatalogRules.optionalText(brandId);
        if (brand != null) criteria.and("brandId").is(brand);
        if (code != null) criteria.and("currency").is(code);
        if (minPrice != null || maxPrice != null) {
            Criteria price = criteria.and("price");
            if (minPrice != null) price.gte(minPrice);
            if (maxPrice != null) price.lte(maxPrice);
        }
        for (Sort.Order sort : pageable.getSort()) {
            if (!SORT_FIELDS.contains(sort.getProperty())) throw CatalogRules.invalid("Unsupported sort field");
            if ("price".equals(sort.getProperty()) && code == null) {
                throw CatalogRules.invalid("Currency is required when sorting by price");
            }
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("createdAt").descending();
        Pageable page = PageRequest.of(pageable.getPageNumber(), Math.min(100, pageable.getPageSize()), sort.and(Sort.by("id")));
        Query query = Query.query(criteria);
        long count = mongo.count(query, Product.class);
        List<ProductResponse> content = mongo.find(query.with(page), Product.class).stream().map(ProductResponse::from).toList();
        return new PageImpl<>(content, page, count);
    }

    private static void validateBound(BigDecimal value) {
        if (value != null && (value.signum() < 0 || (long) value.precision() - value.scale() > 12 || value.scale() > 4)) {
            throw CatalogRules.invalid("Invalid price bound");
        }
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
    }
}
