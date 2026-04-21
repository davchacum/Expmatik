package com.expmatik.backend.maintenanceDetail.DTOs;

import java.time.LocalDateTime;

import com.expmatik.backend.maintenanceDetail.MaintenanceDetail;
import com.expmatik.backend.product.DTOs.ProductResponse;
import com.expmatik.backend.vendingSlot.DTOs.VendingSlotResponse;

public record MaintenanceDetailResponse(
    Integer quantityToRestock,

    LocalDateTime expirationDate,

    VendingSlotResponse vendingSlot,

    ProductResponse product
) {

    public static MaintenanceDetailResponse fromMaintenanceDetail(MaintenanceDetail maintenanceDetail) {
        return new MaintenanceDetailResponse(
            maintenanceDetail.getQuantityToRestock(),
            maintenanceDetail.getExpirationDate(),
            VendingSlotResponse.fromVendingSlot(maintenanceDetail.getVendingSlot()),
            ProductResponse.fromProduct(maintenanceDetail.getProduct())
        );
    }

}
