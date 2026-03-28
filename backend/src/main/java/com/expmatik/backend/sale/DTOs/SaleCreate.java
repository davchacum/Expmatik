package com.expmatik.backend.sale.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.expmatik.backend.sale.PaymentMethod;
import com.expmatik.backend.sale.TransactionStatus;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

public record SaleCreate(

    @NotNull
    @PastOrPresent
    LocalDateTime saleDate,

    @NotNull
    @Digits(integer = 10, fraction = 2)
    BigDecimal totalAmount,

    @NotNull
    @Enumerated(EnumType.STRING)
    PaymentMethod paymentMethod,

    @NotNull
    @Enumerated(EnumType.STRING)
    TransactionStatus status,

    @NotNull
    String barcode,

    @NotNull
    UUID vendingSlotId

) {

}
