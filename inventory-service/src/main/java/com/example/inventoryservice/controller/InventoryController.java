package com.example.inventoryservice.controller;

import com.example.inventoryservice.service.InventoryService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static com.example.inventoryservice.dto.InventoryModels.*;

@RestController
public class InventoryController {
    private final InventoryService service;
    public InventoryController(InventoryService service) { this.service = service; }

    // Public: shoppers only need availability, not warehouse totals.
    @GetMapping("/api/v1/stock/{sku}")
    public Availability stock(@PathVariable String sku) {
        Stock stock = service.stock(sku);
        return new Availability(stock.sku(), stock.available());
    }

    @GetMapping("/internal/v1/inventory/stock/{sku}")
    public Stock internalStock(@PathVariable String sku) { return service.stock(sku); }

    @PostMapping("/internal/v1/inventory/receipts")
    public Stock receive(@Valid @RequestBody Receipt request) { return service.receive(request); }

    @PostMapping("/internal/v1/inventory/reservations")
    public Reservation reserve(@Valid @RequestBody Reserve request) { return service.reserve(request); }

    @GetMapping("/internal/v1/inventory/reservations/{id}")
    public Reservation get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping("/internal/v1/inventory/reservations/{id}/commit")
    public Reservation commit(@PathVariable UUID id) { return service.commit(id); }

    @PostMapping("/internal/v1/inventory/reservations/{id}/release")
    public Reservation release(@PathVariable UUID id) { return service.release(id); }
}
