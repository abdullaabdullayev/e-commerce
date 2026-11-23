package com.example.orderservice.service;

import com.example.orderservice.model.*;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import static com.example.orderservice.dto.OrderModels.*;
import static com.example.orderservice.model.OrderStatus.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderTransactions {
    private final OrderRepository orders;

    public Optional<Order> find(UUID id) { return orders.findById(id).map(this::view); }

    public Order require(UUID id) { return find(id).orElseThrow(OrderTransactions::missing); }

    public Order owned(UUID id, String subject) {
        return orders.findByIdAndCustomerSubject(id, subject).map(this::view).orElseThrow(OrderTransactions::missing);
    }

    @Transactional
    public Order create(UUID id, String subject, String fingerprint, List<Item> items, String currency) {
        orders.lockCheckout(id.toString());
        var order = orders.findById(id).orElseGet(() -> {
            var lines = items.stream().map(i -> new OrderLine(i.productId(), i.sku(), i.name(), i.unitPrice(), i.quantity())).toList();
            return orders.save(new CustomerOrder(id, subject, fingerprint, currency, lines));
        });
        return view(order);
    }

    @Transactional
    public void transition(UUID id, OrderStatus from, OrderStatus to) { lock(id).transition(from, to); }

    @Transactional
    public void pending(UUID id, Instant deadline) { lock(id).awaitPayment(deadline); }

    @Transactional
    public void requestCancellation(UUID id, OrderStatus target) { lock(id).cancel(target); }

    @Transactional
    public void acceptPayment(UUID id, Payment payment) {
        var order = lock(id);
        if (order.getAmount().compareTo(payment.amount()) != 0 || !order.getCurrency().equals(payment.currency())) {
            throw conflict("Payment amount or currency does not match order");
        }
        if (order.getPaymentReference() != null) {
            if (!order.getPaymentReference().equals(payment.reference())) throw conflict("Order already has another payment");
            return;
        }
        if (order.getStatus() == RESERVING) throw conflict("Checkout is not ready for payment");
        order.recordPayment(payment.reference());
    }

    public List<UUID> work() {
        return orders.work(List.of(RESERVING, CONFIRMING, CANCELLING), Instant.now(), PageRequest.of(0, 50));
    }

    public List<Order> list(String subject, int page) {
        return orders.findByCustomerSubjectOrderByCreatedAtDescIdAsc(subject, PageRequest.of(page, 20))
                .stream().map(this::view).toList();
    }

    private CustomerOrder lock(UUID id) { return orders.lockById(id).orElseThrow(OrderTransactions::missing); }

    private Order view(CustomerOrder o) {
        var items = o.getItems().stream().map(i -> new Item(i.getProductId(), i.getSku(), i.getName(), i.getUnitPrice(), i.getQuantity())).toList();
        return new Order(o.getId(), o.getCustomerSubject(), o.getRequestFingerprint(), o.getStatus(),
                o.getAmount(), o.getCurrency(), o.getExpiresAt(), o.getPaymentReference(),
                o.getCancelAs(), o.getCreatedAt(), items);
    }

    private static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
