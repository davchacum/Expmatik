package com.expmatik.backend.maintenanceDetail;

import java.time.LocalDate;

import com.expmatik.backend.model.BaseEntity;
import com.expmatik.backend.product.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
public class MaintenanceDetail extends BaseEntity {
    @NotNull
    @Positive
    @Column(name = "quantity_to_restock", nullable = false)
    private Integer quantityToRestock;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "quantity_restocked")
    private Integer quantityRestocked;

    @Column(name = "quantity_returned")
    private Integer quantityReturned;

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
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
