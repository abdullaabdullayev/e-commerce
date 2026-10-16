package com.example.productservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
@EnableMongoAuditing
public class MangoConfig {

    @Bean
    MongoCustomConversions customConversions() {
        return MongoCustomConversions.create(c ->
                c.bigDecimal(MongoCustomConversions.BigDecimalRepresentation.DECIMAL128));
    }
}
