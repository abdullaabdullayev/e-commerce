package com.example.courseservice.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;

public record Actor(long id, Set<String> roles) {
    public static Actor from(Jwt jwt) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return new Actor(((Number) jwt.getClaim("accountId")).longValue(), Set.copyOf(jwt.getClaimAsStringList("roles")));
    }
    public boolean admin() { return roles.contains("ADMIN"); }
    public boolean teacher() { return roles.contains("TEACHER"); }
    public boolean student() { return roles.contains("STUDENT"); }
    public void requireAdmin() { if (!admin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN); }
}
