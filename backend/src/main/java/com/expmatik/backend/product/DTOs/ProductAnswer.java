package com.expmatik.backend.product.DTOs;

import java.util.List;

import com.expmatik.backend.product.Product;

public record ProductAnswer(String name, String brand, String description, String imageUrl, Boolean isPerishable, String barcode, Boolean isCustom, String createdByEmail) {

    public static ProductAnswer fromProduct(Product product) {
        return new ProductAnswer(
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

    public static List<ProductAnswer> fromProductList(List<Product> products) {
        return products.stream().map(ProductAnswer::fromProduct).toList();
    }
    
}
