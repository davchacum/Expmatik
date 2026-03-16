package com.expmatik.backend.invoice.DTOs;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import com.expmatik.backend.batch.DTOs.BatchResponse;
import com.expmatik.backend.invoice.Invoice;
import com.expmatik.backend.invoice.InvoiceStatus;

public record InvoiceResponse(
    UUID id,
    LocalDate invoiceDate,
    String invoiceNumber,
    InvoiceStatus status,
    String supplierName,
    List<BatchResponse> batches, 
    double totalAmount
) {
    public static InvoiceResponse fromInvoice(Invoice invoice) {
        return new InvoiceResponse(
            invoice.getId(),
            invoice.getInvoiceDate(),
            invoice.getInvoiceNumber(),
            invoice.getStatus(),
            invoice.getSupplier().getName(),
            invoice.getBatch().stream()
                    .map(BatchResponse::fromBatch)
                    .collect(Collectors.toList()),
            invoice.getTotalAmount()
        );
    }
    
    public static List<InvoiceResponse> fromInvoiceList(List<Invoice> invoices) {
        return invoices.stream()
            .map(InvoiceResponse::fromInvoice)
            .collect(Collectors.toList());
    }

    public static Page<InvoiceResponse> fromInvoicePage(Page<Invoice> invoicePage) {
        return invoicePage.map(InvoiceResponse::fromInvoice);
    }
}
