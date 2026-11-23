package com.example.inventoryservice.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpiry {
    private final InventoryService service;
    public ReservationExpiry(InventoryService service) { this.service = service; }

    @Scheduled(fixedDelayString = "${inventory.expiry-poll-ms:1000}")
    public void expire() { service.dueReservations().forEach(service::expire); }
}
