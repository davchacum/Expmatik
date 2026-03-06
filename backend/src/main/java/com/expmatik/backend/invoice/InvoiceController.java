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

import com.expmatik.backend.batch.DTOs.BatchCreate;
import com.expmatik.backend.batch.DTOs.BatchValidationResponse;
import com.expmatik.backend.invoice.DTOs.InvoiceRequest;
import com.expmatik.backend.invoice.DTOs.InvoiceRequestUpdate;
import com.expmatik.backend.invoice.DTOs.InvoiceResponse;
import com.expmatik.backend.product.ProductService;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices", description = "Endpoints para gestionar facturas")
@Validated
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final ProductService productService;
    private final UserService userService;

    public InvoiceController(InvoiceService invoiceService, ProductService productService, UserService userService) {
        this.invoiceService = invoiceService;
        this.productService = productService;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> getInvoiceById(UUID id) {
        User user = userService.getUserProfile();
        Invoice invoice = invoiceService.findInvoiceById(id, user.getId());
        return ResponseEntity.ok(InvoiceResponse.fromInvoice(invoice));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> getAllInvoices() {
        User user = userService.getUserProfile();
        return ResponseEntity.ok(InvoiceResponse.fromInvoiceList(invoiceService.getAllInvoices(user.getId())));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Buscar facturas con filtros opcionales", 
               description = "Todos los parámetros son opcionales. Puedes filtrar por estado, rango de fechas, rango de precios (precio total de la factura), número de factura o proveedor")
    public ResponseEntity<List<InvoiceResponse>> searchInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        User user = userService.getUserProfile();
        return ResponseEntity.ok(InvoiceResponse.fromInvoiceList(invoiceService.searchInvoices(user.getId(), status, startDate, endDate, invoiceNumber, supplierName, minPrice, maxPrice)));
    }

    @GetMapping("/number/{invoiceNumber}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> getInvoiceByInvoiceNumber(String invoiceNumber) {
        User user = userService.getUserProfile();
        Invoice invoice = invoiceService.findInvoiceByInvoiceNumber(invoiceNumber, user.getId());
        return ResponseEntity.ok(InvoiceResponse.fromInvoice(invoice));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> createInvoice(@RequestBody @Valid InvoiceRequest invoiceRequest) {
        
        User user = userService.getUserProfile();

        List<String> barcodes = invoiceRequest.batches()
        .stream()
        .map(BatchCreate::productBarcode)
        .toList();

        BatchValidationResponse validation = productService.validateBarcodes(barcodes, user.getId());

        if (!validation.notFound().isEmpty()) {
            return ResponseEntity.badRequest().body(validation);
        }

        Invoice invoice = invoiceService.createInvoice(user, invoiceRequest);
        return ResponseEntity.ok(InvoiceResponse.fromInvoice(invoice));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> updateInvoiceStatus(UUID id, InvoiceStatus status) {
        User user = userService.getUserProfile();
        Invoice updatedInvoice = invoiceService.updateInvoiceStatus(id, status, user.getId());
        return ResponseEntity.ok(InvoiceResponse.fromInvoice(updatedInvoice));
    }

    @DeleteMapping("/{invoiceNumber}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> deleteInvoice(String invoiceNumber) {
        User user = userService.getUserProfile();
        invoiceService.deleteInvoice(invoiceNumber, user.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> updateInvoice(UUID id, @RequestBody @Valid InvoiceRequestUpdate invoiceRequest) {
        User user = userService.getUserProfile();
        Invoice updatedInvoice = invoiceService.updateInvoice(id, invoiceRequest, user.getId());
        return ResponseEntity.ok(InvoiceResponse.fromInvoice(updatedInvoice));
    }

}
