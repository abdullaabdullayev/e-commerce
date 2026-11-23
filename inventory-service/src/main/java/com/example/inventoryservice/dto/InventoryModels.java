package com.example.inventoryservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class InventoryModels {
    private InventoryModels() {}
    public record Line(@NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,79}") String sku,
                       @Min(1) @Max(1000) int quantity) {}
    public record Receipt(@NotNull UUID id, @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,79}") String sku,
                          @Min(1) @Max(1000000) int quantity) {}
    public record Reserve(@NotNull UUID id, @NotEmpty @Size(max = 50) List<@NotNull @Valid Line> items) {}
    public record Stock(String sku, long onHand, long reserved, long available) {}
    public record Reservation(UUID id, String status, Instant expiresAt, List<Line> items) {}
}
