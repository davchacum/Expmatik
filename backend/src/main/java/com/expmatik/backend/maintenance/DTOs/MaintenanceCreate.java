package com.expmatik.backend.maintenance.DTOs;

import java.time.LocalDate;
import java.util.List;

import com.expmatik.backend.maintenanceDetail.DTOs.MaintenanceDetailCreate;

import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.FutureOrPresent;
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
    
    @NotNull
    @OneToMany
    List<MaintenanceDetailCreate> maintenanceDetails

) {

}
