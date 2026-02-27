package com.expmatik.backend.sale;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.expmatik.backend.model.BaseEntity;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.vendingSlot.VendingSlot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Sale extends BaseEntity {

    @NotNull
    @PastOrPresent
    @Column(name="sale_date",nullable = false, updatable = false)
    private LocalDateTime saleDate;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @Column(name="total_amount",nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name="payment_method",nullable = false)
    private PaymentMethod paymentMethod;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name="status",nullable = false)
    private TransactionStatus status;

    @NotNull
    @ManyToOne
    @JoinColumn(name="product_id",nullable = false)
    private Product product;

    @NotNull
    @ManyToOne
    @JoinColumn(name="vending_slot_id",nullable = false)
    private VendingSlot vendingSlot;


}
