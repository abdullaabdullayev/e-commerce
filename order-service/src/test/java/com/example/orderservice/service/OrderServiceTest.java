package com.example.orderservice.service;

import com.example.orderservice.integration.InventoryClient;
import com.example.orderservice.integration.ProductClient;
import com.example.orderservice.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.example.orderservice.dto.OrderModels.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private final OrderTransactions store = mock(OrderTransactions.class);
    private final InventoryClient inventory = mock(InventoryClient.class);
    private final OrderService service = new OrderService(store, mock(ProductClient.class), inventory);

    private Order reserving(UUID id, Instant createdAt) {
        return new Order(id, "a@x.az", "f", OrderStatus.RESERVING, BigDecimal.TEN, "AZN", null, null, null, createdAt,
                List.of(new Item("p", "SKU-1", "Kit", BigDecimal.TEN, 1)));
    }

    @Test
    void staleReservingOrderIsCancelledInsteadOfRetriedForever() {
        UUID id = UUID.randomUUID();
        when(store.require(id)).thenReturn(reserving(id, Instant.now().minus(OrderService.RESERVING_TIMEOUT).minusSeconds(1)));
        service.advance(id);
        verify(store).requestCancellation(id, OrderStatus.REJECTED);
        verify(inventory, never()).reserve(any(), any());
    }

    @Test
    void freshReservingOrderStillReserves() {
        UUID id = UUID.randomUUID();
        when(store.require(id)).thenReturn(reserving(id, Instant.now()));
        Instant deadline = Instant.now().plusSeconds(300);
        when(inventory.reserve(eq(id), any())).thenReturn(new Reservation(id, "ACTIVE", deadline));
        service.advance(id);
        verify(store).pending(id, deadline);
        verify(store, never()).requestCancellation(any(), any());
    }
}
