package com.example.inventoryservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stock")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockItem {
    @Id @Column(length = 80)
    private String sku;
    private long onHand;
    private long reserved;

    public StockItem(String sku) { this.sku = sku; }

    public long available() { return onHand - reserved; }
    public void receive(int quantity) { onHand += quantity; }
    public void reserve(int quantity) { reserved += quantity; }
    public void release(int quantity) { reserved -= quantity; }
    public void commit(int quantity) {
        reserved -= quantity;
        onHand -= quantity;
    }
}
