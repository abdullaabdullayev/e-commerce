package com.example.inventoryservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "stock_receipts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReceipt {
    @Id private UUID id;
    @Column(length = 80) private String sku;
    private int quantity;

    public StockReceipt(UUID id, String sku, int quantity) {
        this.id = id;
        this.sku = sku;
        this.quantity = quantity;
    }
}
