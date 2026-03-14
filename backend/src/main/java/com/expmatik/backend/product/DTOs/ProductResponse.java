package com.expmatik.backend.product.DTOs;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.expmatik.backend.product.Product;

public record ProductResponse(UUID id, String name, String brand, String description, String imageUrl, Boolean isPerishable, String barcode, Boolean isCustom, String createdByEmail) {

    public static ProductResponse fromProduct(Product product) {
        return new ProductResponse(
            product.getId(),
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

    public static Page<ProductResponse> fromProductPage(Page<Product> productPage) {
        return productPage.map(ProductResponse::fromProduct);
    }
}
