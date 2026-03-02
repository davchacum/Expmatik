package com.expmatik.backend.product.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductImageRequest(
        @NotBlank(message = "Image URL cannot be blank")
        @Size(max = 1000, message = "Image URL cannot exceed 1000 characters")
        String imageUrl
) {
}
