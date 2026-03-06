package com.expmatik.backend.batch.DTOs;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BatchValidationRequest(
    @NotNull @NotEmpty List<String> barcodes
) {
}
