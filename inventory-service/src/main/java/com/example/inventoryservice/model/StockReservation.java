package com.example.inventoryservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservation {
    @Id private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(length = 20) private ReservationStatus status;
    private Instant expiresAt;

    @ElementCollection
    @CollectionTable(name = "reservation_items", joinColumns = @JoinColumn(name = "reservation_id"))
    @OrderBy("sku asc")
    private List<ReservationLine> items = new ArrayList<>();

    public StockReservation(UUID id, ReservationStatus status, Instant expiresAt, List<ReservationLine> items) {
        this.id = id;
        this.status = status;
        this.expiresAt = expiresAt;
        this.items.addAll(items);
    }

    // Match the lock order used when reserving, regardless of database collation.
    public List<ReservationLine> itemsInSkuOrder() {
        return items.stream().sorted(Comparator.comparing(ReservationLine::getSku)).toList();
    }

    public boolean activeAt(Instant now) {
        return status == ReservationStatus.ACTIVE && expiresAt.isAfter(now);
    }

    public void finish(ReservationStatus status) { this.status = status; }
}
