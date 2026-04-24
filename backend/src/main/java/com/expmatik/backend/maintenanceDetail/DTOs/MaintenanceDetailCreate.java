package com.expmatik.backend.maintenanceDetail.DTOs;

import java.time.LocalDate;

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
    LocalDate expirationDate,

    @NotNull
    @Positive
    Integer rowNumber,

    @NotNull
    @Positive
    Integer columnNumber,

    @NotNull
    @ValidBarcode
    String barcode
) {

    public static MaintenanceDetailCreate fromMaintenanceDetail(MaintenanceDetail maintenanceDetail) {
        return new MaintenanceDetailCreate(
            maintenanceDetail.getQuantityToRestock(),
            maintenanceDetail.getExpirationDate(),
            maintenanceDetail.getRowNumber(),
            maintenanceDetail.getColumnNumber(),
            maintenanceDetail.getProduct().getBarcode()
        );
    }

}
