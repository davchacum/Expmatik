package com.expmatik.backend.product.DTOs;

import com.expmatik.backend.product.Product;
import com.expmatik.backend.validation.ValidBarcode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductCreate(
    @NotBlank
    @Size(max = 100)
    String name,

    @NotBlank
    @Size(max = 100)
    String brand,

    @Size(max = 1000, min = 0)
    String description,

    Boolean isPerishable,

    @NotBlank
    @ValidBarcode
    String barcode
) {
    public Product toEntity() {
        Product product = new Product();
        product.setName(this.name);
        product.setBrand(this.brand);
        product.setDescription(this.description);
        product.setIsPerishable(this.isPerishable);
        product.setBarcode(this.barcode);
        return product;
    }


    
}
