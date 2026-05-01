package com.expmatik.backend.prediction.DTOs;

import com.expmatik.backend.validation.ValidBarcode;
import jakarta.validation.constraints.NotNull;

public record PredictionRequest(
        @NotNull
        @ValidBarcode
        String barcode
) {}
