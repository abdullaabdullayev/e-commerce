package com.example.productservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Set;

/**
 * Catalog writes require an Auth Service access token with the ADMIN role.
 * The static operator key remains accepted for scripts and automation.
 */
@Component
public class CatalogAdministrationFilter extends OncePerRequestFilter {
    private final String adminKey;
    private final JwtParser jwtParser;
    private final ObjectMapper mapper;

    public CatalogAdministrationFilter(@Value("${catalog.admin-key:}") String adminKey,
                                       @Value("${catalog.jwt-secret:}") String jwtSecret, ObjectMapper mapper) {
        this.adminKey = adminKey;
        byte[] secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secret.length > 0 && secret.length < 32) throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        this.jwtParser = secret.length == 0 ? null : Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(secret)).build();
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path.startsWith("/internal/v1/catalog/")) return false;
        boolean catalog = path.equals("/api/v1/products") || path.startsWith("/api/v1/products/") ||
                path.equals("/api/v1/categories") || path.startsWith("/api/v1/categories/");
        return !catalog || Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (adminKey.isBlank() && jwtParser == null) {
            reject(response, HttpStatus.SERVICE_UNAVAILABLE, "Catalog administration is not configured");
            return;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            if (isAdminToken(authorization.substring(7))) {
                chain.doFilter(request, response);
            } else {
                reject(response, HttpStatus.FORBIDDEN, "Administrator role is required");
            }
            return;
        }
        if (validKey(request.getHeader("X-Catalog-Admin-Key"))) {
            chain.doFilter(request, response);
            return;
        }
        reject(response, HttpStatus.UNAUTHORIZED, "Sign in as an administrator");
    }

    private boolean isAdminToken(String token) {
        if (jwtParser == null) return false;
        try {
            Claims claims = jwtParser.parseClaimsJws(token).getBody();
            return claims.getExpiration() != null && claims.get("roles") instanceof List<?> roles && roles.contains("ADMIN");
        } catch (JwtException | IllegalArgumentException invalid) {
            return false;
        }
    }

    private boolean validKey(String supplied) {
        return !adminKey.isBlank() && supplied != null && MessageDigest.isEqual(
                adminKey.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }

    private void reject(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detail));
    }
}
