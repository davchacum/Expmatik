package com.expmatik.backend.vendingSlot;

import com.expmatik.backend.model.BaseEntity;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.vendingMachine.VendingMachine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VendingSlot extends BaseEntity{

    @NotNull
    @Max(100)
    @Column(name = "max_capacity",nullable = false,updatable = false)
    private Integer maxCapacity;

    @NotNull
    @PositiveOrZero
    @Column(name = "current_stock",nullable = false)
    private Integer currentStock;

    @NotNull
    @Column(name = "is_blocked",nullable = false)
    private Boolean isBlocked;

    @NotNull
    @Positive
    @Column(name = "row_number",nullable = false,updatable = false)
    private Integer rowNumber;

    @NotNull
    @Positive
    @Column(name = "column_number",nullable = false,updatable = false)
    private Integer columnNumber;
    
    @NotNull
    @ManyToOne
    @JoinColumn(name = "vending_machine_id", nullable = false)
    private VendingMachine vendingMachine;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = true)
    private Product product;
}
