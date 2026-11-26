package com.example.productservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import com.example.productservice.model.Category;
import com.example.productservice.model.Product;
import com.example.productservice.repository.CategoryRepository;
import com.example.productservice.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

    // Demo catalog only with SPRING_PROFILES_ACTIVE=demo; never in production.
    @Bean
    @Profile("demo")
    CommandLineRunner demoSeed(CategoryRepository categories, ProductRepository products) {
        return args -> {
            Category art = categories.findBySlug("yaradiciliq")
                    .orElseGet(() -> categories.save(Category.builder().name("Yaradıcılıq dəstləri").slug("yaradiciliq").build()));
            Category science = categories.findBySlug("elm")
                    .orElseGet(() -> categories.save(Category.builder().name("Elm dəstləri").slug("elm").build()));
            // Fixed IDs match the product references in the order-service demo seed.
            seedProduct(products, "prod-demo-001", "EDU-KIT-001", "Riyaziyyat başlanğıc dəsti", art.getId(), "29.90");
            seedProduct(products, "prod-demo-002", "EDU-KIT-002", "Elm təcrübələri qutusu", science.getId(), "24.50");
            seedProduct(products, "prod-demo-003", "EDU-KIT-003", "Rəsm və rəng dəsti", art.getId(), "19.00");
        };
    }

    private static void seedProduct(ProductRepository repo, String id, String sku, String name, String categoryId, String price) {
        if (!repo.existsBySku(sku)) {
            repo.save(Product.builder().id(id).name(name).description("Demo məhsul").sku(sku).categoryId(categoryId)
                    .brandId("demo").price(new BigDecimal(price)).currency("AZN")
                    .images(List.of()).attributes(Map.of("demo", true)).active(true).build());
        }
    }

}
