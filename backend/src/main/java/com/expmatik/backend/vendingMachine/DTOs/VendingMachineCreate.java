package com.expmatik.backend.vendingMachine.DTOs;

import com.expmatik.backend.vendingMachine.VendingMachine;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record VendingMachineCreate(

    @NotBlank
    @Size(max = 100)
    String location,

    @NotBlank
    @Size(max = 100)
    String name,

    @NotNull
    @Positive
    Integer columnCount,

    @NotNull
    @Positive
    Integer rowCount,

    @NotNull
    @PositiveOrZero
    Integer maxCapacityPerSlot
) {

    public static VendingMachine toEntity(VendingMachineCreate vendingMachineCreate) {
        VendingMachine vendingMachine = new VendingMachine();
        vendingMachine.setLocation(vendingMachineCreate.location);
        vendingMachine.setName(vendingMachineCreate.name);
        vendingMachine.setColumnCount(vendingMachineCreate.columnCount);
        vendingMachine.setRowCount(vendingMachineCreate.rowCount);
        return vendingMachine;
    }

}
