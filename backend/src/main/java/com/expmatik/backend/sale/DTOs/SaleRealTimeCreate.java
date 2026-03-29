package com.expmatik.backend.sale.DTOs;

import java.util.UUID;

import com.expmatik.backend.sale.PaymentMethod;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;

public record SaleRealTimeCreate(

    @NotNull
    @Enumerated(EnumType.STRING)
    PaymentMethod paymentMethod,

    @NotNull
    UUID vendingSlotId

) {

}
