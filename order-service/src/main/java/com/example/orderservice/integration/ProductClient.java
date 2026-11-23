package com.example.orderservice.integration;

import com.example.orderservice.dto.OrderModels.ProductQuote;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProductClient {
    private final RestClient client;

    public ProductClient(@Value("${orders.product-url}") String url, SimpleClientHttpRequestFactory factory) {
        client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    public ProductQuote quote(String id) {
        return client.get().uri("/api/v1/products/{id}", id).retrieve().body(ProductQuote.class);
    }
}
