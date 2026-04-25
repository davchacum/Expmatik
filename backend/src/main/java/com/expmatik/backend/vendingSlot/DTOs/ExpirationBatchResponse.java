package com.expmatik.backend.vendingSlot.DTOs;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.expmatik.backend.vendingSlot.ExpirationBatch;

public record ExpirationBatchResponse(

    UUID id,

    LocalDate expirationDate,

    Integer quantity,

    VendingSlotResponse vendingSlot
) {

    public static ExpirationBatchResponse fromExpirationBatch(ExpirationBatch batch) {
        return new ExpirationBatchResponse(
            batch.getId(),
            batch.getExpirationDate(),
            batch.getQuantity(),
            VendingSlotResponse.fromVendingSlot(batch.getVendingSlot())
        );
    }

    public static List<ExpirationBatchResponse> fromExpirationBatchList(List<ExpirationBatch> batches) {
        return batches.stream().map(ExpirationBatchResponse::fromExpirationBatch).toList();
    }

}
