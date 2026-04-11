package com.expmatik.backend.sale.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.expmatik.backend.sale.PaymentMethod;
import com.expmatik.backend.sale.Sale;
import com.expmatik.backend.sale.TransactionStatus;

public record SaleResponse(

    UUID id,
    LocalDateTime saleDate,
    BigDecimal totalAmount,
    PaymentMethod paymentMethod,
    TransactionStatus status,
    String failureReason,

    UUID productId,
    String barcode,
    String productName,
    String productBrand,

    UUID vendingSlotId,
    Integer rowNumber,
    Integer columnNumber,

    UUID vendingMachineId,
    String machineName

) {

    public static SaleResponse fromSale(Sale sale) {
        return new SaleResponse(
            sale.getId(),
            sale.getSaleDate(),
            sale.getTotalAmount(),
            sale.getPaymentMethod(),
            sale.getStatus(),
            sale.getFailureReason(),
            sale.getProduct().getId(),
            sale.getProduct().getBarcode(),
            sale.getProduct().getName(),
            sale.getProduct().getBrand(),
            sale.getVendingSlot().getId(),
            sale.getVendingSlot().getRowNumber(),
            sale.getVendingSlot().getColumnNumber(),
            sale.getVendingSlot().getVendingMachine().getId(),
            sale.getVendingSlot().getVendingMachine().getName()
        );
    }

    public static Page<SaleResponse> fromSalePage(Page<Sale> salePage) {
        return salePage.map(SaleResponse::fromSale);
    }

}
