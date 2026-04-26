package com.expmatik.backend.sale.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.expmatik.backend.sale.PaymentMethod;
import com.expmatik.backend.sale.TransactionStatus;
import com.expmatik.backend.validation.ValidBarcode;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SaleCreate(

    @NotNull
    @PastOrPresent
    LocalDateTime saleDate,

    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 2)
    BigDecimal totalAmount,

    @NotNull
    @Enumerated(EnumType.STRING)
    PaymentMethod paymentMethod,

    @NotNull
    @Enumerated(EnumType.STRING)
    TransactionStatus status,

    @NotNull
    @ValidBarcode
    String barcode,

    @NotBlank
    @Size(max = 100)
    String machineName,

    @NotNull
    @Positive
    Integer rowNumber,

    @NotNull
    @Positive
    Integer columnNumber

) {

}
