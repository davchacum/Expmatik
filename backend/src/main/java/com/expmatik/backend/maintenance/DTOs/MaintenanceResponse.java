package com.expmatik.backend.maintenance.DTOs;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.expmatik.backend.maintenance.Maintenance;
import com.expmatik.backend.maintenance.MaintenanceStatus;
import com.expmatik.backend.maintenanceDetail.DTOs.MaintenanceDetailResponse;
import com.expmatik.backend.user.DTOs.UserProfile;
import com.expmatik.backend.vendingMachine.DTOs.VendingMachineResponse;

public record MaintenanceResponse(

    UUID id,

    LocalDate maintenanceDate,

    MaintenanceStatus status,

    String description,

    UserProfile maintainer,

    UserProfile administrator,
    
    List<MaintenanceDetailResponse> maintenanceDetails,

    VendingMachineResponse vendingMachine
) {

    public static MaintenanceResponse fromMaintenance(Maintenance maintenance) {
        return new MaintenanceResponse(
            maintenance.getId(),
            maintenance.getMaintenanceDate(),
            maintenance.getStatus(),
            maintenance.getDescription(),
            UserProfile.fromUser(maintenance.getMaintainer()),
            UserProfile.fromUser(maintenance.getAdministrator()),
            maintenance.getMaintenanceDetails().stream()
                .map(MaintenanceDetailResponse::fromMaintenanceDetail)
                .toList(),
            VendingMachineResponse.fromVendingMachine(maintenance.getVendingMachine())
        );
    }

    public static Page<MaintenanceResponse> fromMaintenancePage(Page<Maintenance> maintenancePage) {
        return maintenancePage.map(MaintenanceResponse::fromMaintenance);
    }

}
