package com.expmatik.backend.vendingMachine.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendingMachineUpdate(

    @NotBlank
    @Size(max = 100)
    String location

) {

}
