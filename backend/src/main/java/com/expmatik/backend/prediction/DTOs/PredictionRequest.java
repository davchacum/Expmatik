package com.expmatik.backend.prediction.DTOs;

import jakarta.validation.constraints.NotBlank;

public record PredictionRequest(
        @NotBlank(message = "El barcode del producto es obligatorio")
        String barcode
) {}
