package com.expmatik.backend.maintenanceDetail.DTOs;

import java.time.LocalDate;

import com.expmatik.backend.maintenanceDetail.MaintenanceDetail;
import com.expmatik.backend.product.DTOs.ProductResponse;

public record MaintenanceDetailResponse(
    Integer quantityToRestock,

    LocalDate expirationDate,

    Integer rowNumber,

    Integer columnNumber,

    ProductResponse product
) {

    public static MaintenanceDetailResponse fromMaintenanceDetail(MaintenanceDetail maintenanceDetail) {
        return new MaintenanceDetailResponse(
            maintenanceDetail.getQuantityToRestock(),
            maintenanceDetail.getExpirationDate(),
            maintenanceDetail.getRowNumber(),
            maintenanceDetail.getColumnNumber(),
            ProductResponse.fromProduct(maintenanceDetail.getProduct())
        );
    }

}
