package com.expmatik.backend.productInfo.DTOs;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductInfoUpdate(

    @NotNull Integer stockQuantity,
    @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal saleUnitPrice,
    @NotNull @DecimalMin(value = "0.0") @DecimalMax(value = "1.0") @Digits(integer = 1, fraction = 2) BigDecimal vatRate

) {

    

}
