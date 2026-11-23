package com.example.inventoryservice.repository;

import com.example.inventoryservice.model.StockItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface StockRepository extends JpaRepository<StockItem, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StockItem s where s.sku = :sku")
    Optional<StockItem> lockBySku(String sku);

    // Also serializes the first insert, when there is no stock or reservation row to lock.
    @Query(value = "select 1 from pg_advisory_xact_lock(hashtextextended(:key, 0))", nativeQuery = true)
    int lockKey(String key);
}
