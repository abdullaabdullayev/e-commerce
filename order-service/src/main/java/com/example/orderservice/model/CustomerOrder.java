package com.example.orderservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static com.example.orderservice.model.OrderStatus.*;

@Entity
@Table(name = "customer_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerOrder {
    @Id private UUID id;
    @Column(length = 320) private String customerSubject;
    @Column(length = 64) private String requestFingerprint;
    @Enumerated(EnumType.STRING) @Column(length = 32) private OrderStatus status;
    @Column(precision = 24, scale = 4) private BigDecimal amount;
    @Column(length = 3) private String currency;
    private Instant expiresAt;
    @Column(length = 200) private String paymentReference;
    @Enumerated(EnumType.STRING) @Column(length = 20) private OrderStatus cancelAs;
    private Instant createdAt;
    private Instant updatedAt;

    @ElementCollection
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    @OrderBy("productId asc")
    @org.hibernate.annotations.BatchSize(size = 20)
    private List<OrderLine> items = new ArrayList<>();

    public CustomerOrder(UUID id, String subject, String fingerprint, String currency, List<OrderLine> items) {
        this.id = id;
        this.customerSubject = subject;
        this.requestFingerprint = fingerprint;
        this.currency = currency;
        this.items.addAll(items);
        this.amount = items.stream().map(OrderLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.createdAt = Instant.now();
        changeTo(RESERVING);
    }

    private void changeTo(OrderStatus next) {
        status = next;
        updatedAt = Instant.now();
    }

    public void transition(OrderStatus expected, OrderStatus next) {
        if (status == expected) changeTo(next);
    }

    public void awaitPayment(Instant deadline) {
        if (status == RESERVING) {
            expiresAt = deadline;
            changeTo(PENDING_PAYMENT);
        }
    }

    public void cancel(OrderStatus target) {
        if (status == RESERVING || status == PENDING_PAYMENT) {
            cancelAs = target;
            changeTo(CANCELLING);
        }
    }

    public void recordPayment(String reference) {
        paymentReference = reference;
        changeTo(status == PENDING_PAYMENT ? CONFIRMING : PAYMENT_REVIEW);
    }
}
