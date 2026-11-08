package com.example.inventoryservice;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalAccessFilter extends OncePerRequestFilter {
    private final String key;
    public InternalAccessFilter(@Value("${inventory.service-key:}") String key) { this.key = key; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String supplied = request.getHeader("X-Inventory-Service-Key");
        if (key.isBlank()) { response.sendError(503, "Inventory service key is not configured"); return; }
        if (supplied == null || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(401, "Invalid inventory service key"); return;
        }
        chain.doFilter(request, response);
    }
}
