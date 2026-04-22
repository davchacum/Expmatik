package com.expmatik.backend.maintenance.DTOs;

import com.expmatik.backend.maintenance.Maintenance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MaintenanceUpdate(

    @NotNull
    @Size(max = 255)
    String description,

    @NotNull
    String maintainerEmail
    
) {

    public static MaintenanceUpdate fromMaintenance(Maintenance maintenance) {
        return new MaintenanceUpdate(
            maintenance.getDescription(),
            maintenance.getMaintainer().getEmail()
        );
    }

}
