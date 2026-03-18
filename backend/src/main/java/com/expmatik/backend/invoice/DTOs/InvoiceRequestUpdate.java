package com.expmatik.backend.invoice.DTOs;

import java.time.LocalDate;

import com.expmatik.backend.invoice.InvoiceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvoiceRequestUpdate(
        @NotBlank String invoiceNumber,
        @NotBlank String supplierName,
        @NotNull InvoiceStatus status,
        @NotNull LocalDate invoiceDate
) {




}
