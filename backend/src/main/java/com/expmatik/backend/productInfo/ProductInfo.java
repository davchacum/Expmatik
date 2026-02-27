package com.expmatik.backend.productInfo;

import java.math.BigDecimal;

import com.expmatik.backend.model.BaseEntity;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_info")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductInfo extends BaseEntity {

    @NotNull
    @PositiveOrZero
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    @Digits(integer = 1, fraction = 2)
    @Column(name = "vat_rate", nullable = false, precision = 3, scale = 2)
    private BigDecimal vatRate;

    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 2)
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public BigDecimal getTotalPrice() {
        if (unitPrice == null || vatRate == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.ONE.add(vatRate));
    }
}
