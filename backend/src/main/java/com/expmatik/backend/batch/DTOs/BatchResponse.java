package com.expmatik.backend.batch.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.expmatik.backend.batch.Batch;

public record BatchResponse(
    UUID id,
    LocalDate expirationDate,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal totalPrice,
    UUID productId,
    String productName,
    String productBarcode,
    String brand,
    UUID invoiceId
) {
    public static BatchResponse fromBatch(Batch batch) {
        return new BatchResponse(
            batch.getId(),
            batch.getExpirationDate(),
            batch.getUnitPrice(),
            batch.getQuantity(),
            batch.getTotalPrice(),
            batch.getProduct().getId(),
            batch.getProduct().getName(),
            batch.getProduct().getBarcode(),
            batch.getProduct().getBrand(),
            batch.getInvoice().getId()
        );
    }
}
