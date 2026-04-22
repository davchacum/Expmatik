package com.expmatik.backend.maintenanceDetail.DTOs;

import java.time.LocalDate;

import com.expmatik.backend.maintenanceDetail.MaintenanceDetail;
import com.expmatik.backend.validation.ValidBarcode;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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
    
    @NotBlank
    @Size(max = 100)
    String vendingMachineName,

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
            maintenanceDetail.getVendingMachine().getName(),
            maintenanceDetail.getProduct().getBarcode()
        );
    }

}
