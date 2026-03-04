package com.expmatik.backend.invoice.DTOs;

import com.expmatik.backend.invoice.InvoiceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvoiceRequest(
        @NotBlank String invoiceNumber,
        @NotBlank String supplierName,
        @NotNull InvoiceStatus status
        // @NotNull List<Batch> batches,
        // @NotNull LocalDate invoiceDate
) {




}
