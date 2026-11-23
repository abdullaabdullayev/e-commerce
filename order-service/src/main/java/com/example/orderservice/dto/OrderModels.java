package com.example.orderservice.dto;

import com.example.orderservice.model.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderModels {
    private OrderModels() {}
    public record RequestedItem(@NotBlank @Size(max = 100) String productId, @Min(1) @Max(1000) int quantity) {}
    public record Checkout(@NotEmpty @Size(max = 50) List<@NotNull @Valid RequestedItem> items) {}
    public record Item(String productId, String sku, String name, BigDecimal unitPrice, int quantity) {}
    public record Order(UUID id, String customerSubject, String requestFingerprint, OrderStatus status,
                        BigDecimal amount, String currency, Instant expiresAt, String paymentReference,
                        OrderStatus cancelAs, Instant createdAt, List<Item> items) {}
    public record Payment(@NotBlank @Size(max = 200) String reference, @NotNull @Positive BigDecimal amount,
                          @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency) {}
    public record ProductQuote(String id, String sku, String name, BigDecimal price, String currency, boolean active) {}
    public record StockLine(String sku, int quantity) {}
    public record StockRequest(UUID id, List<StockLine> items) {}
    public record Reservation(UUID id, String status, Instant expiresAt) {}
}
