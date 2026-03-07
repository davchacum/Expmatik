package com.expmatik.backend.productInfo.DTOs;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.expmatik.backend.productInfo.ProductInfo;

public record ProductInfoResponse(

    UUID productId,
    String productName,
    Integer stockQuantity,
    BigDecimal saleUnitPrice,
    BigDecimal lastPurchaseUnitPrice,
    BigDecimal vatRate,
    BigDecimal totalStockValue,
    BigDecimal lastPurchaseUnitPriceWithVat,
    BigDecimal unitProfit,
    BigDecimal totalProfit
    
) {

    public static ProductInfoResponse fromProductInfo(ProductInfo productInfo) {
        return new ProductInfoResponse(
            productInfo.getProduct().getId(),
            productInfo.getProduct().getName(),
            productInfo.getStockQuantity(),
            productInfo.getSaleUnitPrice(),
            productInfo.getLastPurchaseUnitPrice(),
            productInfo.getVatRate(),
            productInfo.getTotalStockValue(),
            productInfo.getLastPurchaseUnitPriceWithVat(),
            productInfo.getUnitProfit(),
            productInfo.getTotalProfit()
        );
    }

    public static List<ProductInfoResponse> fromProductInfoList(List<ProductInfo> productInfos) {
        return productInfos.stream().map(ProductInfoResponse::fromProductInfo).toList();
    }

}
