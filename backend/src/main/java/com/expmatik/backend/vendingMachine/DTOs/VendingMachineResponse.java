package com.expmatik.backend.vendingMachine.DTOs;

import java.util.UUID;

import org.springframework.data.domain.Page;

import com.expmatik.backend.vendingMachine.VendingMachine;

public record VendingMachineResponse(

    UUID id,
    String location,
    String name,
    Integer columnCount,
    Integer rowCount,
    UUID userId

) {

    public static VendingMachineResponse fromVendingMachine(VendingMachine vendingMachine) {
        return new VendingMachineResponse(
            vendingMachine.getId(),
            vendingMachine.getLocation(),
            vendingMachine.getName(),
            vendingMachine.getColumnCount(),
            vendingMachine.getRowCount(),
            vendingMachine.getUser().getId()
        );
    }
    
    public static Page<VendingMachineResponse> fromVendingMachinePage(Page<VendingMachine> vendingMachinePage) {
        return vendingMachinePage.map(VendingMachineResponse::fromVendingMachine);
    }

}
