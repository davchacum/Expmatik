package com.expmatik.backend.product.DTOs;

import java.util.List;

import com.expmatik.backend.product.Product;

public record ProductResponse(String name, String brand, String description, String imageUrl, Boolean isPerishable, String barcode, Boolean isCustom, String createdByEmail) {

    public static ProductResponse fromProduct(Product product) {
        return new ProductResponse(
            product.getName(),
            product.getBrand(),
            product.getDescription(),
            product.getImageUrl(),
            product.getIsPerishable(),
            product.getBarcode(),
            product.getIsCustom(),
            product.getCreatedBy() != null ? product.getCreatedBy().getEmail() : null
        );
    }

    public static List<ProductResponse> fromProductList(List<Product> products) {
        return products.stream().map(ProductResponse::fromProduct).toList();
    }
    
}
