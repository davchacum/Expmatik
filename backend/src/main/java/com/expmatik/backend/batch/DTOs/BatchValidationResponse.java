package com.expmatik.backend.batch.DTOs;

import java.util.List;

public record BatchValidationResponse(
    List<String> valid,
    List<String> notFound,
    String message
) {
    public static BatchValidationResponse of(List<String> valid, List<String> notFound) {
        int notFoundCount = notFound.size();
        String message = notFoundCount == 0 
            ? "All barcodes are valid."
            : notFoundCount + " barcode(s) not found.";

        return new BatchValidationResponse(valid, notFound, message);
    }
}
