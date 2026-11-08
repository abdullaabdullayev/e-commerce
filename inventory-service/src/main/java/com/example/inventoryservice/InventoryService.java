package com.example.inventoryservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static com.example.inventoryservice.InventoryModels.*;

@Service
public class InventoryService {
    private final JdbcTemplate db;
    private final long reservationSeconds;

    public InventoryService(JdbcTemplate db, @Value("${inventory.reservation-seconds:300}") long seconds) {
        if (seconds < 1 || seconds > 3600) throw new IllegalArgumentException("Invalid inventory reservation duration");
        this.db = db;
        this.reservationSeconds = seconds;
    }

    public Stock stock(String sku) {
        return db.query("SELECT * FROM stock WHERE sku=?", (rs, row) -> new Stock(rs.getString("sku"),
                rs.getLong("on_hand"), rs.getLong("reserved"), rs.getLong("on_hand") - rs.getLong("reserved")), sku)
                .stream().findFirst().orElse(new Stock(sku, 0, 0, 0));
    }

    @Transactional
    public Stock receive(Receipt request) {
        lock("receipt:" + request.id());
        List<Receipt> prior = db.query("SELECT * FROM stock_receipts WHERE id=?", (rs, row) ->
                new Receipt(rs.getObject("id", UUID.class), rs.getString("sku"), rs.getInt("quantity")), request.id());
        if (!prior.isEmpty()) {
            if (!prior.getFirst().equals(request)) throw conflict("Receipt ID was used with different contents");
            return stock(request.sku());
        }
        int changed = db.update("""
                INSERT INTO stock(sku,on_hand) VALUES (?,?)
                ON CONFLICT(sku) DO UPDATE SET on_hand=stock.on_hand+EXCLUDED.on_hand
                WHERE stock.on_hand+EXCLUDED.on_hand <= 1000000000000
                """, request.sku(), request.quantity());
        if (changed != 1) throw conflict("Stock limit exceeded");
        db.update("INSERT INTO stock_receipts(id,sku,quantity) VALUES (?,?,?)",
                request.id(), request.sku(), request.quantity());
        return stock(request.sku());
    }

    @Transactional
    public Reservation reserve(Reserve request) {
        lock("reservation:" + request.id());
        SortedMap<String, Integer> quantities = new TreeMap<>();
        request.items().forEach(line -> quantities.merge(line.sku(), line.quantity(), Integer::sum));
        List<Line> items = quantities.entrySet().stream().map(e -> new Line(e.getKey(), e.getValue())).toList();
        Optional<Reservation> prior = find(request.id());
        if (prior.isPresent()) {
            Reservation old = prior.get();
            if (!old.items().equals(items)) throw conflict("Reservation ID was used with different items");
            if (old.status().equals("COMMITTED")) return old;
            if (!old.status().equals("ACTIVE") || !old.expiresAt().isAfter(Instant.now())) {
                throw conflict("Reservation is no longer available");
            }
            return old; // A retry never extends the original hold.
        }
        Instant expiresAt = Instant.now().plusSeconds(reservationSeconds);
        db.update("INSERT INTO reservations(id,status,expires_at) VALUES (?,'ACTIVE',?)", request.id(), Timestamp.from(expiresAt));
        for (Line line : items) {
            int changed = db.update("UPDATE stock SET reserved=reserved+? WHERE sku=? AND on_hand-reserved>=?",
                    line.quantity(), line.sku(), line.quantity());
            if (changed != 1) throw conflict("Insufficient available stock for " + line.sku());
            db.update("INSERT INTO reservation_items(reservation_id,sku,quantity) VALUES (?,?,?)",
                    request.id(), line.sku(), line.quantity());
        }
        return new Reservation(request.id(), "ACTIVE", expiresAt, items);
    }

    @Transactional(readOnly = true)
    public Reservation get(UUID id) {
        return find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
    }

    @Transactional
    public Reservation commit(UUID id) {
        lock("reservation:" + id);
        Reservation reservation = get(id);
        if (reservation.status().equals("COMMITTED")) return reservation;
        if (!reservation.status().equals("ACTIVE") || !reservation.expiresAt().isAfter(Instant.now())) {
            throw conflict("Reservation expired or was released; payment requires reconciliation");
        }
        for (Line line : reservation.items()) {
            db.update("UPDATE stock SET on_hand=on_hand-?, reserved=reserved-? WHERE sku=?",
                    line.quantity(), line.quantity(), line.sku());
        }
        db.update("UPDATE reservations SET status='COMMITTED' WHERE id=?", id);
        return get(id);
    }

    @Transactional
    public Reservation release(UUID id) {
        lock("reservation:" + id);
        Optional<Reservation> reservation = find(id);
        if (reservation.isEmpty()) {
            // A cancellation arriving before reserve prevents a late in-flight reserve.
            db.update("INSERT INTO reservations(id,status,expires_at) VALUES (?,'RELEASED',CURRENT_TIMESTAMP)", id);
            return get(id);
        }
        return finishRelease(reservation.get(), "RELEASED");
    }

    @Transactional
    public void expire(UUID id) {
        lock("reservation:" + id);
        find(id).filter(r -> r.status().equals("ACTIVE") && !r.expiresAt().isAfter(Instant.now()))
                .ifPresent(r -> finishRelease(r, "EXPIRED"));
    }

    public List<UUID> dueReservations() {
        return db.query("SELECT id FROM reservations WHERE status='ACTIVE' AND expires_at<=CURRENT_TIMESTAMP ORDER BY expires_at LIMIT 100",
                (rs, row) -> rs.getObject(1, UUID.class));
    }

    private Reservation finishRelease(Reservation reservation, String status) {
        if (reservation.status().equals("COMMITTED")) throw conflict("Committed stock cannot be released; use a return workflow");
        if (!reservation.status().equals("ACTIVE")) return reservation;
        for (Line line : reservation.items()) {
            db.update("UPDATE stock SET reserved=reserved-? WHERE sku=?", line.quantity(), line.sku());
        }
        db.update("UPDATE reservations SET status=? WHERE id=?", status, reservation.id());
        return get(reservation.id());
    }

    private Optional<Reservation> find(UUID id) {
        return db.query("SELECT * FROM reservations WHERE id=?", (rs, row) -> new Reservation(id,
                rs.getString("status"), rs.getTimestamp("expires_at").toInstant(),
                db.query("SELECT sku,quantity FROM reservation_items WHERE reservation_id=? ORDER BY sku", (item, n) ->
                        new Line(item.getString("sku"), item.getInt("quantity")), id).stream()
                        .sorted(Comparator.comparing(Line::sku)).toList()), id).stream().findFirst();
    }

    private void lock(String key) {
        // Transaction-scoped PostgreSQL advisory lock also serializes first insert for an ID.
        // A hash collision only adds waiting; it cannot bypass the stock constraints.
        db.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", rs -> { }, key);
    }

    private static ResponseStatusException conflict(String detail) {
        return new ResponseStatusException(HttpStatus.CONFLICT, detail);
    }
}
