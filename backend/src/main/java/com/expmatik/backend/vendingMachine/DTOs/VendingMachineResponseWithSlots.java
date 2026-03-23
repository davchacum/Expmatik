package com.expmatik.backend.vendingMachine.DTOs;

import java.util.List;
import java.util.UUID;

import com.expmatik.backend.vendingMachine.VendingMachine;
import com.expmatik.backend.vendingSlot.VendingSlot;
import com.expmatik.backend.vendingSlot.DTOs.VendingSlotResponse;

public record VendingMachineResponseWithSlots(

    UUID id,
    String location,
    String name,
    Integer columnCount,
    Integer rowCount,
    UUID userId,
    List<VendingSlotResponse> vendingSlots

) {

    public static VendingMachineResponseWithSlots fromVendingMachine(VendingMachine vendingMachine,List<VendingSlot> vendingSlots) {
        return new VendingMachineResponseWithSlots(
            vendingMachine.getId(),
            vendingMachine.getLocation(),
            vendingMachine.getName(),
            vendingMachine.getColumnCount(),
            vendingMachine.getRowCount(),
            vendingMachine.getUser().getId(),
            VendingSlotResponse.fromVendingSlotList(vendingSlots)
        );
    }


}
