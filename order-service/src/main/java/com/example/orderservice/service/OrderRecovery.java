package com.example.orderservice.service;

import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderRecovery {
    private final OrderTransactions store;
    private final OrderService service;
    public OrderRecovery(OrderTransactions store, OrderService service) { this.store = store; this.service = service; }
    @Scheduled(fixedDelayString = "${orders.recovery-poll-ms:2000}")
    public void recover() {
        for (var id : store.work()) {
            try { service.advance(id); }
            catch (RuntimeException e) { LoggerFactory.getLogger(OrderRecovery.class).warn("Order {} recovery failed ({})", id, e.getClass().getSimpleName()); }
        }
    }
}
