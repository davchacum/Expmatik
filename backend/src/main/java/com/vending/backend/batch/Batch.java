package com.vending.backend.batch;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.vending.backend.model.BaseEntity;
import com.vending.backend.product.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Batch extends BaseEntity {

    @NotNull
    @Column(name="expiration_date",nullable = false)
    private LocalDateTime expirationDate;

    @NotNull
    @Positive
    @Column(name="unit_price",nullable = false, precision = 12, scale = 2)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal unitPrice;

    @NotNull
    @Positive
    @Column(name="quantity",nullable = false)
    private Integer quantity;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

}
