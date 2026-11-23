package com.example.orderservice.integration;

import org.springframework.context.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
class SalesHttpConfig {
    @Bean
    SimpleClientHttpRequestFactory salesRequestFactory() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        return factory;
    }
}
