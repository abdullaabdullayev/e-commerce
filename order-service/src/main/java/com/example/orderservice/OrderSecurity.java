package com.example.orderservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class OrderSecurity {
    @Bean
    JwtDecoder jwtDecoder(@Value("${orders.jwt-secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        // Match JJWT's current signWith(key) selection in Auth Service.
        MacAlgorithm algorithm = bytes.length >= 64 ? MacAlgorithm.HS512 : bytes.length >= 48 ? MacAlgorithm.HS384 : MacAlgorithm.HS256;
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(bytes, "HmacSHA" + algorithm.getName().substring(2)))
                .macAlgorithm(algorithm).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), jwt -> {
            if (jwt.getSubject() == null || jwt.getSubject().isBlank() || jwt.getSubject().length() > 320 || jwt.getExpiresAt() == null) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing token identity or expiration", null));
            }
            return OAuth2TokenValidatorResult.success();
        }));
        return decoder;
    }

    @Bean
    SecurityFilterChain orderSecurity(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/internal/**", "/error").permitAll().anyRequest().authenticated())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> { })).build();
    }
}
