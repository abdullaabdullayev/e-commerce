package com.example.productservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Boots the full application context against a disposable MongoDB, including index creation for the catalog documents. */
@SpringBootTest(properties = "eureka.client.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class ProductServiceApplicationTests {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongo.getReplicaSetUrl("product_db"));
    }

    @Test
    void contextLoads() {
    }
}
