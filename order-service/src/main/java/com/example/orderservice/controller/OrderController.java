package com.example.orderservice.controller;

import com.example.orderservice.service.OrderService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.example.orderservice.dto.OrderModels.*;

@RestController
@Validated
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @PostMapping("/api/v1/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView checkout(Authentication authentication, @RequestHeader("Idempotency-Key") UUID key,
                              @Valid @RequestBody Checkout request) {
        return OrderView.from(service.checkout(authentication.getName(), key, request));
    }

    @GetMapping("/api/v1/orders")
    public List<OrderView> list(Authentication authentication, @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
        return service.list(authentication.getName(), page).stream().map(OrderView::from).toList();
    }

    @GetMapping("/api/v1/orders/{id}")
    public OrderView get(Authentication authentication, @PathVariable UUID id) {
        return OrderView.from(service.owned(id, authentication.getName()));
    }

    @PostMapping("/api/v1/orders/{id}/cancel")
    public OrderView cancel(Authentication authentication, @PathVariable UUID id) {
        return OrderView.from(service.cancel(id, authentication.getName()));
    }

    @PostMapping("/internal/v1/orders/{id}/payment-confirmed")
    public OrderView paid(@PathVariable UUID id, @Valid @RequestBody Payment payment) {
        return OrderView.from(service.paid(id, payment));
    }
}
