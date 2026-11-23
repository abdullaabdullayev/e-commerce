package com.example.inventoryservice.service;

import com.example.inventoryservice.model.*;
import com.example.inventoryservice.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import static com.example.inventoryservice.dto.InventoryModels.*;
import static com.example.inventoryservice.model.ReservationStatus.*;

@Service
@Transactional(readOnly = true)
public class InventoryService {
    private final StockRepository stock;
    private final StockReceiptRepository receipts;
    private final ReservationRepository reservations;
    private final long holdSeconds;

    public InventoryService(StockRepository stock, StockReceiptRepository receipts, ReservationRepository reservations,
                            @Value("${inventory.reservation-seconds:300}") long holdSeconds) {
        if (holdSeconds < 1 || holdSeconds > 3600) throw new IllegalArgumentException("Invalid reservation duration");
        this.stock = stock;
        this.receipts = receipts;
        this.reservations = reservations;
        this.holdSeconds = holdSeconds;
    }

    public Stock stock(String sku) {
        return stock.findById(sku).map(this::view).orElse(new Stock(sku, 0, 0, 0));
    }

    @Transactional
    public Stock receive(Receipt request) {
        stock.lockKey("receipt:" + request.id());
        var existing = receipts.findById(request.id());
        if (existing.isPresent()) {
            var receipt = existing.get();
            if (!receipt.getSku().equals(request.sku()) || receipt.getQuantity() != request.quantity()) {
                throw conflict("Receipt ID was used with different contents");
            }
            return stock(request.sku());
        }
        stock.lockKey("stock:" + request.sku());
        var item = stock.lockBySku(request.sku()).orElseGet(() -> new StockItem(request.sku()));
        if (item.getOnHand() + request.quantity() > 1_000_000_000_000L) throw conflict("Stock limit exceeded");
        item.receive(request.quantity());
        stock.saveAndFlush(item);
        receipts.save(new StockReceipt(request.id(), request.sku(), request.quantity()));
        return view(item);
    }

    @Transactional
    public Reservation reserve(Reserve request) {
        lockReservation(request.id());
        var quantities = new TreeMap<String, Integer>();
        request.items().forEach(line -> quantities.merge(line.sku(), line.quantity(), Integer::sum));
        var lines = quantities.entrySet().stream().map(e -> new Line(e.getKey(), e.getValue())).toList();
        var prior = reservations.findById(request.id());
        if (prior.isPresent()) {
            var previous = prior.get();
            Reservation response = view(previous);
            if (!response.items().equals(lines)) throw conflict("Reservation ID was used with different items");
            if (previous.getStatus() != COMMITTED && !previous.activeAt(Instant.now())) {
                throw conflict("Reservation is no longer available");
            }
            return response;
        }
        List<ReservationLine> items = new ArrayList<>();
        for (Line line : lines) {
            var item = lockStock(line.sku());
            if (item.available() < line.quantity()) throw conflict("Insufficient available stock for " + line.sku());
            item.reserve(line.quantity());
            items.add(new ReservationLine(line.sku(), line.quantity()));
        }
        return view(reservations.save(new StockReservation(request.id(), ACTIVE, Instant.now().plusSeconds(holdSeconds), items)));
    }

    public Reservation get(UUID id) { return view(require(id)); }

    @Transactional
    public Reservation commit(UUID id) {
        lockReservation(id);
        var reservation = require(id);
        if (reservation.getStatus() == COMMITTED) return view(reservation);
        if (!reservation.activeAt(Instant.now())) throw conflict("Reservation expired or was released; payment requires reconciliation");
        for (var line : reservation.itemsInSkuOrder()) lockStock(line.getSku()).commit(line.getQuantity());
        reservation.finish(COMMITTED);
        return view(reservation);
    }

    @Transactional
    public Reservation release(UUID id) {
        lockReservation(id);
        var existing = reservations.findById(id);
        if (existing.isEmpty()) {
            // An early cancellation must also stop a delayed first reserve.
            return view(reservations.save(new StockReservation(id, RELEASED, Instant.now(), List.of())));
        }
        var reservation = existing.get();
        if (reservation.getStatus() == COMMITTED) throw conflict("Committed stock cannot be released; use a return workflow");
        if (reservation.getStatus() == ACTIVE) releaseItems(reservation, RELEASED);
        return view(reservation);
    }

    @Transactional
    public void expire(UUID id) {
        lockReservation(id);
        reservations.findById(id).filter(r -> r.getStatus() == ACTIVE && !r.activeAt(Instant.now()))
                .ifPresent(r -> releaseItems(r, EXPIRED));
    }

    public List<UUID> dueReservations() { return reservations.due(Instant.now(), PageRequest.of(0, 100)); }

    private void releaseItems(StockReservation reservation, ReservationStatus status) {
        for (var line : reservation.itemsInSkuOrder()) lockStock(line.getSku()).release(line.getQuantity());
        reservation.finish(status);
    }

    private StockReservation require(UUID id) {
        return reservations.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
    }

    private void lockReservation(UUID id) { stock.lockKey("reservation:" + id); }

    private StockItem lockStock(String sku) {
        return stock.lockBySku(sku).orElseThrow(() -> conflict("Insufficient available stock for " + sku));
    }

    private Stock view(StockItem item) {
        return new Stock(item.getSku(), item.getOnHand(), item.getReserved(), item.available());
    }

    private Reservation view(StockReservation reservation) {
        return new Reservation(reservation.getId(), reservation.getStatus().name(), reservation.getExpiresAt(),
                reservation.itemsInSkuOrder().stream().map(i -> new Line(i.getSku(), i.getQuantity())).toList());
    }

    private static ResponseStatusException conflict(String detail) {
        return new ResponseStatusException(HttpStatus.CONFLICT, detail);
    }
}
