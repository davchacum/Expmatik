package com.expmatik.backend.batch;

import java.beans.Transient;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.expmatik.backend.invoice.Invoice;
import com.expmatik.backend.model.BaseEntity;
import com.expmatik.backend.product.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
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
    @FutureOrPresent
    @Column(name="expiration_date")
    private LocalDate expirationDate;

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

    @NotNull
    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Transient
    public BigDecimal getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

}
