package com.example.orderservice;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.sql.Timestamp;
import java.util.*;
import static com.example.orderservice.OrderModels.*;

@Repository
public class OrderStore {
    private final JdbcTemplate db;
    public OrderStore(JdbcTemplate db) { this.db = db; }

    @Transactional(readOnly = true)
    public Optional<Order> find(UUID id) {
        return db.query("SELECT * FROM customer_orders WHERE id=?", (rs, row) -> new Order(id,
                rs.getString("customer_subject"), rs.getString("request_fingerprint"), rs.getString("status"),
                rs.getBigDecimal("amount"), rs.getString("currency"),
                rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toInstant(),
                rs.getString("payment_reference"), rs.getString("cancel_as"), rs.getTimestamp("created_at").toInstant(),
                db.query("SELECT * FROM order_items WHERE order_id=? ORDER BY product_id", (item, n) -> new Item(
                        item.getString("product_id"), item.getString("sku"), item.getString("name"),
                        item.getBigDecimal("unit_price"), item.getInt("quantity")), id)), id).stream().findFirst();
    }

    @Transactional(readOnly = true)
    public Order require(UUID id) {
        return find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    @Transactional(readOnly = true)
    public Order owned(UUID id, String subject) {
        Order order = require(id);
        if (!order.customerSubject().equals(subject)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        return order;
    }

    @Transactional
    public Order create(UUID id, String subject, String fingerprint, List<Item> items, String currency) {
        db.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", rs -> { }, id.toString());
        Optional<Order> prior = find(id);
        if (prior.isPresent()) return prior.get();
        var amount = items.stream().map(i -> i.unitPrice().multiply(java.math.BigDecimal.valueOf(i.quantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        db.update("INSERT INTO customer_orders(id,customer_subject,request_fingerprint,status,amount,currency) VALUES (?,?,?,'RESERVING',?,?)",
                id, subject, fingerprint, amount, currency);
        for (Item item : items) db.update("INSERT INTO order_items(order_id,product_id,sku,name,unit_price,quantity) VALUES (?,?,?,?,?,?)",
                id, item.productId(), item.sku(), item.name(), item.unitPrice(), item.quantity());
        return require(id);
    }

    public boolean transition(UUID id, String from, String to) {
        return db.update("UPDATE customer_orders SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status=?", to, id, from) == 1;
    }

    public void pending(UUID id, java.time.Instant expiresAt) {
        db.update("UPDATE customer_orders SET status='PENDING_PAYMENT',expires_at=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='RESERVING'",
                Timestamp.from(expiresAt), id);
    }

    public void requestCancellation(UUID id, String target) {
        db.update("UPDATE customer_orders SET status='CANCELLING',cancel_as=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status IN ('RESERVING','PENDING_PAYMENT')",
                target, id);
    }

    @Transactional
    public Order acceptPayment(UUID id, Payment payment) {
        db.queryForList("SELECT id FROM customer_orders WHERE id=? FOR UPDATE", id);
        Order order = require(id);
        if (order.amount().compareTo(payment.amount()) != 0 || !order.currency().equals(payment.currency())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment amount or currency does not match order");
        }
        if (order.paymentReference() != null) {
            if (!order.paymentReference().equals(payment.reference())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Order already has another payment");
            return order;
        }
        if (order.status().equals("RESERVING")) throw new ResponseStatusException(HttpStatus.CONFLICT, "Checkout is not ready for payment");
        String next = order.status().equals("PENDING_PAYMENT") ? "CONFIRMING" : "PAYMENT_REVIEW";
        db.update("UPDATE customer_orders SET status=?,payment_reference=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", next, payment.reference(), id);
        return require(id);
    }

    public List<UUID> work() {
        return db.query("""
                SELECT id FROM customer_orders WHERE status IN ('RESERVING','CONFIRMING','CANCELLING')
                OR (status='PENDING_PAYMENT' AND expires_at<=CURRENT_TIMESTAMP)
                ORDER BY updated_at LIMIT 50
                """, (rs, row) -> rs.getObject(1, UUID.class));
    }

    @Transactional(readOnly = true)
    public List<Order> list(String subject, int page) {
        return db.query("SELECT id FROM customer_orders WHERE customer_subject=? ORDER BY created_at DESC,id LIMIT 20 OFFSET ?",
                (rs, row) -> require(rs.getObject(1, UUID.class)), subject, (long) page * 20);
    }
}
