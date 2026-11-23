package com.example.inventoryservice.repository;

import com.example.inventoryservice.model.StockReservation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import java.util.*;

public interface ReservationRepository extends JpaRepository<StockReservation, UUID> {
    @Query("""
        select r.id from StockReservation r
        where r.status = com.example.inventoryservice.model.ReservationStatus.ACTIVE and r.expiresAt <= :now
        order by r.expiresAt, r.id
        """)
    List<UUID> due(Instant now, Pageable page);
}
