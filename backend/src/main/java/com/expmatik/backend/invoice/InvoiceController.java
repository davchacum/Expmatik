package com.expmatik.backend.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expmatik.backend.invoice.DTOs.InvoiceRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices", description = "Endpoints para gestionar facturas")
@Validated
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> getInvoiceById(UUID id) {
        Invoice invoice = invoiceService.findInvoiceById(id);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Buscar facturas con filtros opcionales", 
               description = "Todos los parámetros son opcionales. Puedes filtrar por estado, rango de fechas, rango de precios (precio total de la factura), número de factura o proveedor")
    public ResponseEntity<List<Invoice>> searchInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return ResponseEntity.ok(invoiceService.searchInvoices(status, startDate, endDate, invoiceNumber, supplierName, minPrice, maxPrice));
    }

    @GetMapping("/number/{invoiceNumber}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> getInvoiceByInvoiceNumber(String invoiceNumber) {
        Invoice invoice = invoiceService.findInvoiceByInvoiceNumber(invoiceNumber);
        return ResponseEntity.ok(invoice);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> createInvoice(@RequestBody @Valid InvoiceRequest invoiceRequest) {
        Invoice invoice = invoiceService.createInvoice(invoiceRequest);
        return ResponseEntity.ok(invoice);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> updateInvoiceStatus(UUID id, InvoiceStatus status) {
        Invoice updatedInvoice = invoiceService.updateInvoiceStatus(id, status);
        return ResponseEntity.ok(updatedInvoice);
    }

    @DeleteMapping("/{invoiceNumber}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> deleteInvoice(String invoiceNumber) {
        invoiceService.deleteInvoice(invoiceNumber);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> updateInvoice(UUID id, @RequestBody @Valid InvoiceRequest invoiceRequest) {
        Invoice updatedInvoice = invoiceService.updateInvoice(id,invoiceRequest);
        return ResponseEntity.ok(updatedInvoice);
    }

}
