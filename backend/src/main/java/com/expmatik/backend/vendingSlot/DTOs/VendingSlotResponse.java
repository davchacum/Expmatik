package com.expmatik.backend.vendingSlot.DTOs;

import java.util.List;
import java.util.UUID;

import com.expmatik.backend.product.DTOs.ProductResponse;
import com.expmatik.backend.vendingSlot.VendingSlot;

public record VendingSlotResponse(

    UUID id,

    Integer maxCapacity,

    Integer currentStock,

    Boolean isBlocked,

    Integer rowNumber,

    Integer columnNumber,
    
    UUID vendingMachineId,

    ProductResponse product

) {

    public static VendingSlotResponse fromVendingSlot(VendingSlot vendingSlot) {
        return new VendingSlotResponse(
            vendingSlot.getId(),
            vendingSlot.getMaxCapacity(),
            vendingSlot.getCurrentStock(),
            vendingSlot.getIsBlocked(),
            vendingSlot.getRowNumber(),
            vendingSlot.getColumnNumber(),
            vendingSlot.getVendingMachine().getId(),
            vendingSlot.getProduct() != null ? ProductResponse.fromProduct(vendingSlot.getProduct()) : null
        );
    }

    public static List<VendingSlotResponse> fromVendingSlotList(List<VendingSlot> vendingSlots) {
        return vendingSlots.stream().map(VendingSlotResponse::fromVendingSlot).toList();
    }

}
