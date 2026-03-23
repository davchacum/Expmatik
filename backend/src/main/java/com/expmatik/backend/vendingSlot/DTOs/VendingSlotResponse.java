package com.expmatik.backend.vendingSlot.DTOs;

import java.util.List;

import com.expmatik.backend.product.DTOs.ProductResponse;
import com.expmatik.backend.vendingMachine.VendingMachine;
import com.expmatik.backend.vendingSlot.ExpirationBatch;
import com.expmatik.backend.vendingSlot.VendingSlot;

public record VendingSlotResponse(

    Integer maxCapacity,

    Integer currentStock,

    Boolean isBlocked,

    Integer rowNumber,

    Integer columnNumber,
    
    VendingMachine vendingMachine,

    List<ExpirationBatch> expirationBatch,

    ProductResponse product

) {

    public static VendingSlotResponse fromVendingSlot(VendingSlot vendingSlot) {
        return new VendingSlotResponse(
            vendingSlot.getMaxCapacity(),
            vendingSlot.getCurrentStock(),
            vendingSlot.getIsBlocked(),
            vendingSlot.getRowNumber(),
            vendingSlot.getColumnNumber(),
            vendingSlot.getVendingMachine(),
            vendingSlot.getExpirationBatch(),
            ProductResponse.fromProduct(vendingSlot.getProduct())
        );
    }

    public static List<VendingSlotResponse> fromVendingSlotList(List<VendingSlot> vendingSlots) {
        return vendingSlots.stream().map(VendingSlotResponse::fromVendingSlot).toList();
    }

}
