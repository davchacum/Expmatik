package com.expmatik.backend.sale;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.expmatik.backend.sale.DTOs.SaleCreate;
import com.expmatik.backend.sale.DTOs.SaleRealTimeCreate;
import com.expmatik.backend.sale.DTOs.SaleResponse;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sales")
@Tag(name = "SaleController", description = "Controlador para gestionar las ventas realizadas en las máquinas expendedoras.")
@Validated
public class SaleController {

    private final SaleService saleService;
    private final UserService userService;

    public SaleController(SaleService saleService, UserService userService) {
        this.saleService = saleService;
        this.userService = userService;
    }

    @GetMapping("{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> getSaleById(@PathVariable UUID id) throws AccessDeniedException {
        User currentUser = userService.getUserProfile();
        return ResponseEntity.ok(SaleResponse.fromSale(saleService.getSaleById(id, currentUser)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> createSale(@RequestBody @Valid SaleCreate saleCreate) throws AccessDeniedException {
        User currentUser = userService.getUserProfile();
        return ResponseEntity.ok(SaleResponse.fromSale(saleService.createSale(saleCreate, currentUser)));
    }

    @PostMapping("/real-time")
    public ResponseEntity<?> realTimeSale(@RequestBody @Valid SaleRealTimeCreate request) throws AccessDeniedException {
        User currentUser = userService.getUserProfile();
        return ResponseEntity.ok(SaleResponse.fromSale(saleService.realTimeSale(request.vendingSlotId(), request.paymentMethod(), currentUser)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> searchSales(@RequestParam(value = "barcode", required = false) String barcode, 
        @RequestParam(required = false) String machineName, 
        @RequestParam(required = false) Integer rowNumber,
        @RequestParam(required = false) Integer columnNumber, 
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate, 
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate, 
        @RequestParam(required = false) PaymentMethod paymentMethod, 
        @RequestParam(required = false) TransactionStatus status, 
        @ParameterObject Pageable pageable) throws AccessDeniedException {
        User currentUser = userService.getUserProfile();
        Page<Sale> salesPage = saleService.searchSales(currentUser.getId(), barcode, machineName, rowNumber, columnNumber, startDate, endDate, paymentMethod, status, pageable);
        return ResponseEntity.ok(SaleResponse.fromSalePage(salesPage));
    }

    @PostMapping(value="/csv" , consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> readSalesFromCSV(@RequestParam("csv") MultipartFile csvContent) throws AccessDeniedException {
        return ResponseEntity.ok(saleService.readSalesFromCSV(csvContent));
    }

    @GetMapping("/csv-export")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> exportSalesToCSV(@RequestParam(value = "barcode", required = false) String barcode, 
        @RequestParam(required = false) String machineName, 
        @RequestParam(required = false) Integer rowNumber, 
        @RequestParam(required = false) Integer columnNumber, 
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate, 
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate, 
        @RequestParam(required = false) PaymentMethod paymentMethod, 
        @RequestParam(required = false) TransactionStatus status) throws AccessDeniedException {
        User currentUser = userService.getUserProfile();
        byte[] csvData = saleService.exportSalesCSV(currentUser.getId(), barcode, machineName, rowNumber, columnNumber, startDate, endDate, paymentMethod, status);
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"sales.csv\"")
            .header("Content-Type", "text/csv")
            .body(csvData);
        }

}
