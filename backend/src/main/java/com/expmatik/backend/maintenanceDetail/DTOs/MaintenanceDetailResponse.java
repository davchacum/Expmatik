package com.expmatik.backend.maintenanceDetail.DTOs;

import java.time.LocalDate;
import java.util.UUID;

import com.expmatik.backend.maintenanceDetail.MaintenanceDetail;
import com.expmatik.backend.product.DTOs.ProductResponse;

public record MaintenanceDetailResponse(

    UUID id,

    Integer quantityToRestock,

    LocalDate expirationDate,

    Integer rowNumber,

    Integer columnNumber,

    ProductResponse product,

    Integer quantityRestocked,

    Integer quantityReturned
) {

    public static MaintenanceDetailResponse fromMaintenanceDetail(MaintenanceDetail maintenanceDetail) {
        return new MaintenanceDetailResponse(
            maintenanceDetail.getId(),
            maintenanceDetail.getQuantityToRestock(),
            maintenanceDetail.getExpirationDate(),
            maintenanceDetail.getRowNumber(),
            maintenanceDetail.getColumnNumber(),
            ProductResponse.fromProduct(maintenanceDetail.getProduct()),
            maintenanceDetail.getQuantityRestocked(),
            maintenanceDetail.getQuantityReturned()
        );
    }

}
