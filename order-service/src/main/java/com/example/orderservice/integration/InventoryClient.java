package com.example.orderservice.integration;

import com.example.orderservice.dto.OrderModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.*;

@Component
public class InventoryClient {
    private static final String RESERVATIONS = "/internal/v1/inventory/reservations";
    private final RestClient client;
    private final boolean configured;

    public InventoryClient(@Value("${orders.inventory-url}") String url, @Value("${orders.inventory-key:}") String key,
                           SimpleClientHttpRequestFactory factory) {
        configured = !key.isBlank();
        client = RestClient.builder().baseUrl(url).requestFactory(factory).defaultHeader("X-Inventory-Service-Key", key).build();
    }

    public boolean configured() { return configured; }

    public Reservation reserve(UUID id, List<Item> items) {
        var lines = items.stream().map(i -> new StockLine(i.sku(), i.quantity())).toList();
        return client.post().uri(RESERVATIONS).body(new StockRequest(id, lines)).retrieve().body(Reservation.class);
    }

    public void commit(UUID id) { client.post().uri(RESERVATIONS + "/{id}/commit", id).retrieve().toBodilessEntity(); }
    public void release(UUID id) { client.post().uri(RESERVATIONS + "/{id}/release", id).retrieve().toBodilessEntity(); }
}
