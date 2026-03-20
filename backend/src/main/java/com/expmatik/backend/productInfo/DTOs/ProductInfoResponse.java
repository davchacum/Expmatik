package com.expmatik.backend.productInfo.DTOs;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.expmatik.backend.productInfo.ProductInfo;

public record ProductInfoResponse(

    UUID productInfoId,
    UUID productId,
    String productName,
    String productBarcode,
    String productBrand,
    Boolean isPerishable,
    Integer stockQuantity,
    BigDecimal saleUnitPrice,
    BigDecimal lastPurchaseUnitPrice,
    BigDecimal vatRate,
    BigDecimal totalStockValue,
    BigDecimal lastPurchaseUnitPriceWithVat,
    BigDecimal unitProfit,
    BigDecimal totalProfit,
    Boolean needUpdate    
) {

    public static ProductInfoResponse fromProductInfo(ProductInfo productInfo) {
        return new ProductInfoResponse(
            productInfo.getId(),
            productInfo.getProduct().getId(),
            productInfo.getProduct().getName(),
            productInfo.getProduct().getBarcode(),
            productInfo.getProduct().getBrand(),
            productInfo.getProduct().getIsPerishable(),
            productInfo.getStockQuantity(),
            productInfo.getSaleUnitPrice(),
            productInfo.getLastPurchaseUnitPrice(),
            productInfo.getVatRate(),
            productInfo.getTotalStockValue(),
            productInfo.getLastPurchaseUnitPriceWithVat(),
            productInfo.getUnitProfit(),
            productInfo.getTotalProfit(),
            productInfo.getNeedUpdate()
        );
    }

    public static List<ProductInfoResponse> fromProductInfoList(List<ProductInfo> productInfos) {
        return productInfos.stream().map(ProductInfoResponse::fromProductInfo).toList();
    }

    public static Page<ProductInfoResponse> fromProductInfoPage(Page<ProductInfo> productInfoPage) {
        List<ProductInfoResponse> content = productInfoPage.getContent().stream()
                .map(ProductInfoResponse::fromProductInfo)
                .toList();
        return new PageImpl<>(content, productInfoPage.getPageable(), productInfoPage.getTotalElements());
    }

}
