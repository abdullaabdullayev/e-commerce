package com.example.courseservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class CourseSecurity {
    @Bean JwtDecoder courseJwtDecoder(@Value("${courses.jwt-secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        MacAlgorithm algorithm = bytes.length >= 64 ? MacAlgorithm.HS512 : bytes.length >= 48 ? MacAlgorithm.HS384 : MacAlgorithm.HS256;
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(bytes, "HmacSHA" + algorithm.getName().substring(2))).macAlgorithm(algorithm).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), jwt -> {
            Object id = jwt.getClaim("accountId"), roles = jwt.getClaim("roles");
            if (!(id instanceof Number n) || n.longValue() <= 0 || n.doubleValue() != n.longValue()
                    || !(roles instanceof List<?> list) || !list.stream().allMatch(String.class::isInstance)
                    || jwt.getExpiresAt() == null || jwt.getSubject() == null) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Log in again to obtain LMS claims", null));
            }
            return OAuth2TokenValidatorResult.success();
        }));
        return decoder;
    }
    @Bean SecurityFilterChain courseSecurityFilterChain(HttpSecurity http) throws Exception {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles"); authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter(); converter.setJwtGrantedAuthoritiesConverter(authorities);
        return http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/parent/**").hasRole("PARENT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/courses", "/api/v1/courses/{id:[0-9a-fA-F-]{36}}").permitAll()
                        .requestMatchers("/error").permitAll().anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))).build();
    }
}
