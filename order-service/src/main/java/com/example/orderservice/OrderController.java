package com.example.orderservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.example.orderservice.OrderModels.*;

@RestController
public class OrderController {
    private final OrderService service;
    private final OrderStore store;
    public OrderController(OrderService service, OrderStore store) { this.service = service; this.store = store; }

    @PostMapping("/api/v1/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Order checkout(Authentication authentication, @RequestHeader("Idempotency-Key") UUID key,
                          @Valid @RequestBody Checkout request) {
        return service.checkout(authentication.getName(), key, request);
    }

    @GetMapping("/api/v1/orders")
    public List<Order> list(Authentication authentication, @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
        return store.list(authentication.getName(), page);
    }

    @GetMapping("/api/v1/orders/{id}")
    public Order get(Authentication authentication, @PathVariable UUID id) { return store.owned(id, authentication.getName()); }

    @PostMapping("/api/v1/orders/{id}/cancel")
    public Order cancel(Authentication authentication, @PathVariable UUID id) { return service.cancel(id, authentication.getName()); }

    @PostMapping("/internal/v1/orders/{id}/payment-confirmed")
    public Order paid(@PathVariable UUID id, @Valid @RequestBody Payment payment) { return service.paid(id, payment); }
}
