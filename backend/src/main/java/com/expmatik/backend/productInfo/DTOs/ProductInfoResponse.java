package com.expmatik.backend.productInfo.DTOs;

import java.math.BigDecimal;
import java.util.UUID;

import com.expmatik.backend.productInfo.ProductInfo;

public record ProductInfoResponse(

    UUID productId,
    Integer stockQuantity,
    BigDecimal unitPrice,
    BigDecimal vatRate,
    String productName,
    UUID createdByUserId
    
) {

    public static ProductInfoResponse fromProductInfo(ProductInfo productInfo) {
        return new ProductInfoResponse(
            productInfo.getProduct().getId(),
            productInfo.getStockQuantity(),
            productInfo.getUnitPrice(),
            productInfo.getVatRate(),
            productInfo.getProduct().getName(),
            productInfo.getProduct().getCreatedBy().getId()
        );
    }

}
