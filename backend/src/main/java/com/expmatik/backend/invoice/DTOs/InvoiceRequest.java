package com.expmatik.backend.invoice.DTOs;

import java.time.LocalDate;
import java.util.List;

import com.expmatik.backend.batch.DTOs.BatchCreate;
import com.expmatik.backend.invoice.InvoiceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvoiceRequest(
        @NotBlank String invoiceNumber,
        @NotBlank String supplierName,
        @NotNull InvoiceStatus status,
        @NotNull List<BatchCreate> batches,
        @NotNull LocalDate invoiceDate
) {




}
