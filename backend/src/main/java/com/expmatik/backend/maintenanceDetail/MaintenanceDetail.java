package com.expmatik.backend.maintenanceDetail;

import java.time.LocalDateTime;

import com.expmatik.backend.model.BaseEntity;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.vendingSlot.VendingSlot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.FutureOrPresent;
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
public class MaintenanceDetail extends BaseEntity {   
    @NotNull
    @Column(name = "quantity_to_restock", nullable = false)
    private Integer quantityToRestock;

    @FutureOrPresent
    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "vending_slot_id", nullable = false)
    private VendingSlot vendingSlot;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
