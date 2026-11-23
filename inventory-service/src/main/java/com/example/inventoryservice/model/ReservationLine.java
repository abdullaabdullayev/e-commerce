package com.example.inventoryservice.model;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReservationLine {
    @Column(nullable = false, length = 80)
    private String sku;
    @Column(nullable = false)
    private int quantity;
}
