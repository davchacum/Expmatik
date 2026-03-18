package com.expmatik.backend.batch.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.expmatik.backend.batch.Batch;
import com.expmatik.backend.validation.ValidBarcode;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BatchCreate(
    LocalDate expirationDate,
    @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
    @NotNull @Positive Integer quantity,
    @NotNull @ValidBarcode String productBarcode
) {

    public BatchCreate(Batch batch) {
        this(
            batch.getExpirationDate(),
            batch.getUnitPrice(),
            batch.getQuantity(),
            batch.getProduct().getBarcode()
        );
    }
}
