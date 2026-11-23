package com.example.inventoryservice.repository;

import com.example.inventoryservice.model.StockReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface StockReceiptRepository extends JpaRepository<StockReceipt, UUID> {}
