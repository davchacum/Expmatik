package com.expmatik.backend.productInfo.DTOs;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.expmatik.backend.productInfo.ProductInfo;

public record ProductInfoResponse(

    UUID productId,
    Integer stockQuantity,
    BigDecimal saleUnitPrice,
    BigDecimal vatRate,
    String productName,
    BigDecimal totalStockValue
    
) {

    public static ProductInfoResponse fromProductInfo(ProductInfo productInfo) {
        return new ProductInfoResponse(
            productInfo.getProduct().getId(),
            productInfo.getStockQuantity(),
            productInfo.getSaleUnitPrice(),
            productInfo.getVatRate(),
            productInfo.getProduct().getName(),
            productInfo.getTotalStockValue()
        );
    }

    public static List<ProductInfoResponse> fromProductInfoList(List<ProductInfo> productInfos) {
        return productInfos.stream().map(ProductInfoResponse::fromProductInfo).toList();
    }

}
