package com.example.inventoryservice;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static com.example.inventoryservice.InventoryModels.*;

@RestController
public class InventoryController {
    private final InventoryService service;
    public InventoryController(InventoryService service) { this.service = service; }

    @GetMapping("/api/v1/stock/{sku}")
    public Stock stock(@PathVariable String sku) { return service.stock(sku); }

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
