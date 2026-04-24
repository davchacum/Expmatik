package com.expmatik.backend.maintenance.DTOs;

import java.time.LocalDate;

import com.expmatik.backend.maintenance.Maintenance;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MaintenanceCreate(

    @NotNull
    @FutureOrPresent
    LocalDate maintenanceDate,

    @NotNull
    @Size(max = 255)
    String description,

    @NotNull
    String maintainerEmail,

    @NotBlank
    @Size(max = 100)
    String vendingMachineName
    
) {

    public static MaintenanceCreate fromMaintenance(Maintenance maintenance) {
        return new MaintenanceCreate(
            maintenance.getMaintenanceDate(),
            maintenance.getDescription(),
            maintenance.getMaintainer().getEmail(),
            maintenance.getVendingMachine().getName()
        );
    }

}
