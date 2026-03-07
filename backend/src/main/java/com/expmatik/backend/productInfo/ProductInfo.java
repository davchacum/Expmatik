package com.expmatik.backend.productInfo;

import java.beans.Transient;
import java.math.BigDecimal;

import com.expmatik.backend.model.BaseEntity;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "product_info",uniqueConstraints = {@UniqueConstraint(columnNames = {"product_id", "user_id"})})
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
    @DecimalMax(value = "0.21")
    @Digits(integer = 1, fraction = 2)
    @Column(name = "vat_rate", nullable = false, precision = 3, scale = 2)
    private BigDecimal vatRate;

    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 2)
    @Column(name = "sale_unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal saleUnitPrice;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Transient
    public BigDecimal getTotalStockValue() {
        if (saleUnitPrice == null || vatRate == null || stockQuantity == null) return BigDecimal.ZERO;
        return saleUnitPrice.multiply(BigDecimal.valueOf(stockQuantity));
    }
}
