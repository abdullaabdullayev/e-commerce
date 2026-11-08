package com.example.productservice.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

final class CatalogRules {
    private CatalogRules() {}

    static ResponseStatusException invalid(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    static String currency(String value) {
        try {
            String code = value.trim().toUpperCase(Locale.ROOT);
            if (Currency.getInstance(code).getDefaultFractionDigits() < 0) throw invalid("Unsupported currency");
            return code;
        } catch (IllegalArgumentException e) {
            throw invalid("Invalid ISO currency code");
        }
    }

    static BigDecimal price(BigDecimal price, String code) {
        if (price == null || price.signum() <= 0 || (long) price.precision() - price.scale() > 12) {
            throw invalid("Price must be positive with at most 12 integer digits");
        }
        try {
            BigDecimal normalized = price.stripTrailingZeros();
            int digits = Currency.getInstance(code).getDefaultFractionDigits();
            if (normalized.scale() > digits) throw invalid("Price has too many decimal places for " + code);
            return normalized.setScale(digits, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw invalid("Price has too many decimal places for " + code);
        }
    }

    static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static List<String> images(List<String> values) {
        if (values == null) return List.of();
        List<String> result = new ArrayList<>();
        for (String value : values) {
            try {
                URI uri = URI.create(value.trim());
                if (uri.getHost() == null || uri.getUserInfo() != null ||
                        !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
                    throw invalid("Images must be absolute HTTP or HTTPS URLs without credentials");
                }
                result.add(uri.toString());
            } catch (IllegalArgumentException e) {
                throw invalid("Invalid image URL");
            }
        }
        return List.copyOf(result);
    }

    static Map<String, Object> attributes(Map<String, Object> value) {
        if (value == null) return Map.of();
        validateAttribute(value, 0, new AtomicInteger(500));
        return new LinkedHashMap<>(value);
    }

    private static void validateAttribute(Object value, int depth, AtomicInteger budget) {
        if (depth > 5 || budget.decrementAndGet() < 0) throw invalid("Attributes are too complex");
        if (value instanceof Map<?, ?> map) {
            if (map.size() > 50) throw invalid("Too many attribute fields");
            for (var entry : map.entrySet()) {
                String key = entry.getKey().toString();
                if (key.isBlank() || key.length() > 64 || key.contains(".") || key.startsWith("$")) {
                    throw invalid("Invalid attribute field name");
                }
                validateAttribute(entry.getValue(), depth + 1, budget);
            }
        } else if (value instanceof List<?> list) {
            if (list.size() > 100) throw invalid("Attribute list is too long");
            list.forEach(item -> validateAttribute(item, depth + 1, budget));
        } else if (value instanceof String text) {
            if (text.length() > 2000) throw invalid("Attribute value is too long");
        } else if (value instanceof Number number) {
            if (!Double.isFinite(number.doubleValue()) || value instanceof java.math.BigInteger) {
                throw invalid("Attribute number is out of range");
            }
        } else if (value != null && !(value instanceof Boolean)) {
            throw invalid("Unsupported attribute value");
        }
    }
}
