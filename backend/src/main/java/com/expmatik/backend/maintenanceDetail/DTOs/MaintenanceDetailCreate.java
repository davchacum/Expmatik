package com.expmatik.backend.maintenanceDetail.DTOs;

import java.time.LocalDateTime;
import java.util.UUID;

import com.expmatik.backend.maintenanceDetail.MaintenanceDetail;
import com.expmatik.backend.validation.ValidBarcode;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MaintenanceDetailCreate(
    @NotNull
    @Positive
    Integer quantityToRestock,

    @FutureOrPresent
    LocalDateTime expirationDate,

    @NotNull
    UUID vendingSlotId,

    @NotNull
    @ValidBarcode
    String barcode
) {

    public static MaintenanceDetailCreate fromMaintenanceDetail(MaintenanceDetail maintenanceDetail) {
        return new MaintenanceDetailCreate(
            maintenanceDetail.getQuantityToRestock(),
            maintenanceDetail.getExpirationDate(),
            maintenanceDetail.getVendingSlot().getId(),
            maintenanceDetail.getProduct().getBarcode()
        );
    }

}
