package com.expmatik.backend.vendingSlot.DTOs;

import java.time.LocalDate;
import java.util.UUID;

import com.expmatik.backend.vendingSlot.ExpirationBatch;

public record ExpirationBatchResponse(

    UUID id,

    LocalDate expirationDate,

    Integer quantity,

    UUID vendingSlotId
) {

    public static ExpirationBatchResponse fromExpirationBatch(ExpirationBatch batch) {
        return new ExpirationBatchResponse(
            batch.getId(),
            batch.getExpirationDate(),
            batch.getQuantity(),
            batch.getVendingSlot().getId()
        );
    }

}
