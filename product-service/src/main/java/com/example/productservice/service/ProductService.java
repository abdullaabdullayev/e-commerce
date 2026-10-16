package com.example.productservice.service;

import com.example.productservice.dto.CreateProductRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.model.Product;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProductService {
    private  final ProductRepository productRepository;

    public ProductResponse create(CreateProductRequest request){
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .sku(request.sku())
                .categoryId(request.categoryId())
                .brandId(request.brandId())
                .price(request.price())
                .currency(request.currency())
                .images(request.images())
                .attributes(request.attributes())
                .active(true)
                .build();

        try {
            return ProductResponse.from(productRepository.save(product));
        }catch (DuplicateKeyException e){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Sku already exists: " + request.sku());
        }
    }
}
