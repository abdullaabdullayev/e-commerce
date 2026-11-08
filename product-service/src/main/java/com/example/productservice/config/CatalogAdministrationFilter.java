package com.example.productservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Set;

/** Operator access until staff roles are implemented in Auth Service. */
@Component
public class CatalogAdministrationFilter extends OncePerRequestFilter {
    private final String adminKey;
    private final ObjectMapper mapper;

    public CatalogAdministrationFilter(@Value("${catalog.admin-key:}") String adminKey, ObjectMapper mapper) {
        this.adminKey = adminKey;
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
        String supplied = request.getHeader("X-Catalog-Admin-Key");
        if (adminKey.isBlank() || supplied == null || !MessageDigest.isEqual(
                adminKey.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            HttpStatus status = adminKey.isBlank() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.UNAUTHORIZED;
            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(status,
                    adminKey.isBlank() ? "Catalog administration is not configured" : "Invalid catalog administrator key"));
            return;
        }
        chain.doFilter(request, response);
    }
}
