package com.vending.backend.vendingSlot;

import java.util.List;

import com.vending.backend.model.BaseEntity;
import com.vending.backend.product.Product;
import com.vending.backend.vendingMachine.VendingMachine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
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
    @Column(name = "current_stock",nullable = false)
    private Integer currentStock;

    @NotNull
    @Column(name = "is_blocked",nullable = false)
    private Boolean isBlocked;

    @NotNull
    @Column(name = "row_number",nullable = false,updatable = false)
    private Integer rowNumber;

    @NotNull
    @Column(name = "column_number",nullable = false,updatable = false)
    private Integer columnNumber;
    
    @NotNull
    @ManyToOne
    @JoinColumn(name = "vending_machine_id", nullable = false)
    private VendingMachine vendingMachine;

    @NotNull
    @OneToMany
    @JoinColumn(name = "vending_slot_id", nullable = false)
    private List<ExpirationBatch> expirationBatch;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
