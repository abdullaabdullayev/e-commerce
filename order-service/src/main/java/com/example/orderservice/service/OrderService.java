package com.example.orderservice.service;

import com.example.orderservice.integration.InventoryClient;
import com.example.orderservice.integration.ProductClient;
import com.example.orderservice.model.OrderStatus;
import static com.example.orderservice.model.OrderStatus.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import static com.example.orderservice.dto.OrderModels.*;

@Service
public class OrderService {
    private static final Set<OrderStatus> PAID_OR_UNDER_REVIEW = Set.of(CONFIRMING, CONFIRMED, PAYMENT_REVIEW);
    static final java.time.Duration RESERVING_TIMEOUT = java.time.Duration.ofMinutes(15);

    private final OrderTransactions store;
    private final ProductClient products;
    private final InventoryClient inventory;

    public OrderService(OrderTransactions store, ProductClient products, InventoryClient inventory) {
        this.store = store;
        this.products = products;
        this.inventory = inventory;
    }

    public List<Order> list(String subject, int page) { return store.list(subject, page); }

    public Order owned(UUID id, String subject) { return store.owned(id, subject); }

    public Order checkout(String subject, UUID key, Checkout request) {
        if (!inventory.configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Checkout inventory access is not configured");
        SortedMap<String, Integer> requested = new TreeMap<>();
        for (RequestedItem item : request.items()) requested.merge(item.productId(), item.quantity(), Integer::sum);
        if (requested.values().stream().anyMatch(q -> q > 1000)) throw bad("Maximum item quantity is 1000");
        String fingerprint = fingerprint(requested);
        UUID id = UUID.nameUUIDFromBytes((subject + "\n" + key).getBytes(StandardCharsets.UTF_8));
        Optional<Order> prior = store.find(id);
        if (prior.isPresent()) return replay(prior.get(), subject, fingerprint);
        List<Item> items = new ArrayList<>();
        String currency = null;
        for (var entry : requested.entrySet()) {
            ProductQuote product;
            try {
                product = products.quote(entry.getKey());
            } catch (HttpClientErrorException.NotFound e) { throw bad("Requested product is unavailable"); }
            if (product == null || !entry.getKey().equals(product.id()) || !product.active() || product.price() == null ||
                    product.price().signum() <= 0 || product.currency() == null || product.sku() == null) {
                throw bad("Requested product is unavailable");
            }
            if (currency != null && !currency.equals(product.currency())) throw bad("Order items must use the same currency");
            currency = product.currency();
            items.add(new Item(product.id(), product.sku(), product.name(), product.price(), entry.getValue()));
        }
        Order order = store.create(id, subject, fingerprint, items, currency);
        replay(order, subject, fingerprint);
        advance(id);
        return store.require(id);
    }

    private Order replay(Order order, String subject, String fingerprint) {
        if (!order.customerSubject().equals(subject) || !order.requestFingerprint().equals(fingerprint)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key was used with different items");
        }
        return order;
    }

    public Order cancel(UUID id, String subject) {
        Order order = store.owned(id, subject);
        if (PAID_OR_UNDER_REVIEW.contains(order.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Paid orders require a refund workflow");
        }
        store.requestCancellation(id, CANCELLED);
        if (PAID_OR_UNDER_REVIEW.contains(store.require(id).status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is already being processed");
        }
        advance(id);
        return store.require(id);
    }

    // Internal caller must verify the provider's payment before invoking this method.
    public Order paid(UUID id, Payment payment) {
        store.acceptPayment(id, payment);
        advance(id);
        return store.require(id);
    }

    public void advance(UUID id) {
        Order order = store.require(id);
        try {
            switch (order.status()) {
                case RESERVING -> {
                    if (order.createdAt().plus(RESERVING_TIMEOUT).isBefore(Instant.now())) {
                        // Inventory stayed unreachable: give up, and release any reservation that did land.
                        store.requestCancellation(id, REJECTED);
                        return;
                    }
                    Reservation reservation = inventory.reserve(id, order.items());
                    if (reservation == null || !reservation.status().equals("ACTIVE")) throw bad("Invalid inventory reservation");
                    store.pending(id, reservation.expiresAt());
                }
                case PENDING_PAYMENT -> {
                    if (order.expiresAt() != null && !order.expiresAt().isAfter(Instant.now())) {
                        store.requestCancellation(id, EXPIRED);
                    }
                }
                case CONFIRMING -> {
                    inventory.commit(id);
                    store.transition(id, CONFIRMING, CONFIRMED);
                }
                case CANCELLING -> {
                    inventory.release(id);
                    store.transition(id, CANCELLING, order.cancelAs());
                }
                default -> { }
            }
        } catch (HttpClientErrorException.Conflict | HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
            if (order.status() == RESERVING) store.transition(id, RESERVING, REJECTED);
            else if (order.status() == CONFIRMING || order.status() == CANCELLING) {
                store.transition(id, order.status(), PAYMENT_REVIEW);
            }
        } catch (RestClientException e) {
            // Durable state remains recoverable. Never keep a DB transaction open for HTTP.
            org.slf4j.LoggerFactory.getLogger(OrderService.class).warn("Order {} waits for inventory ({})", id, e.getClass().getSimpleName());
        }
    }

    private static String fingerprint(SortedMap<String, Integer> items) {
        try {
            // Length-prefix IDs so arbitrary input cannot create ambiguous encodings.
            StringBuilder text = new StringBuilder();
            items.forEach((id, qty) -> text.append(id.length()).append(':').append(id).append(':').append(qty).append(';'));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
