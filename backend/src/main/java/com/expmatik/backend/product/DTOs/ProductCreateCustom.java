package com.expmatik.backend.product.DTOs;

import org.springframework.web.multipart.MultipartFile;

import com.expmatik.backend.product.Product;
import com.expmatik.backend.user.User;
import com.expmatik.backend.validation.ValidBarcode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductCreateCustom(
    @NotBlank
    @Size(max = 100)
    String name,

    @NotBlank
    @Size(max = 100)
    String brand,

    @Size(max = 1000, min = 0)
    String description,

    @NotNull
    Boolean isPerishable,

    @NotBlank
    @ValidBarcode
    String barcode,

    @NotNull
    MultipartFile file,

    @NotNull
    User currentUser
) {
    public Product toEntity() {
        Product product = new Product();
        product.setName(this.name);
        product.setBrand(this.brand);
        product.setDescription(this.description);
        product.setIsPerishable(this.isPerishable);
        product.setBarcode(this.barcode);
        product.setIsCustom(true);
        product.setCreatedBy(this.currentUser);
        return product;
    }
}
