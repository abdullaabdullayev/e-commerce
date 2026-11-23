package com.example.orderservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OrderLine {
    @Column(length = 100, nullable = false) private String productId;
    @Column(length = 80, nullable = false) private String sku;
    @Column(length = 200, nullable = false) private String name;
    @Column(precision = 16, scale = 4, nullable = false) private BigDecimal unitPrice;
    @Column(nullable = false) private int quantity;

    public BigDecimal subtotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
