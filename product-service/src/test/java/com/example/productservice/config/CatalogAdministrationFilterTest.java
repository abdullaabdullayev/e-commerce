package com.example.productservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogAdministrationFilterTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef-test";
    private final CatalogAdministrationFilter filter =
            new CatalogAdministrationFilter("operator-key", SECRET, new ObjectMapper());

    private static String token(List<String> roles, long ttlMs) {
        return Jwts.builder().setSubject("someone@x.az").claim("roles", roles)
                .setExpiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private int post(String header, String value) throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/products");
        request.setServletPath("/api/v1/products");
        if (header != null) request.addHeader(header, value);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return chain.getRequest() != null ? 200 : response.getStatus();
    }

    @Test
    void adminTokenIsAccepted() throws Exception {
        assertThat(post("Authorization", "Bearer " + token(List.of("ADMIN"), 60_000))).isEqualTo(200);
    }

    @Test
    void nonAdminOrExpiredTokenIsRejected() throws Exception {
        assertThat(post("Authorization", "Bearer " + token(List.of("CUSTOMER"), 60_000))).isEqualTo(403);
        assertThat(post("Authorization", "Bearer " + token(List.of("ADMIN"), -60_000))).isEqualTo(403);
        assertThat(post("Authorization", "Bearer not-a-jwt")).isEqualTo(403);
    }

    @Test
    void operatorKeyStillWorksAndMissingCredentialsAreRejected() throws Exception {
        assertThat(post("X-Catalog-Admin-Key", "operator-key")).isEqualTo(200);
        assertThat(post("X-Catalog-Admin-Key", "wrong")).isEqualTo(401);
        assertThat(post(null, null)).isEqualTo(401);
    }

    @Test
    void readsAreNotFiltered() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.setServletPath("/api/v1/products");
        var chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(chain.getRequest()).isNotNull();
    }
}
