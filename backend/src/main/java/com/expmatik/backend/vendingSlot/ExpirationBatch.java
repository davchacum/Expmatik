package com.expmatik.backend.vendingSlot;

import java.time.LocalDate;

import com.expmatik.backend.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
public class ExpirationBatch extends BaseEntity {

    @NotNull
    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vending_slot_id", nullable = false)
    private VendingSlot vendingSlot;
}
