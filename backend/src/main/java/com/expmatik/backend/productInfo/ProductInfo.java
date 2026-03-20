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
import jakarta.persistence.Transient;
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

    @Positive
    @Digits(integer = 10, fraction = 2)
    @Column(name = "last_purchase_unit_price", precision = 12, scale = 2)
    private BigDecimal lastPurchaseUnitPrice;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "need_update", nullable = false)
    private Boolean needUpdate;

    @Transient
    public BigDecimal getTotalStockValue() {
        return saleUnitPrice.multiply(BigDecimal.valueOf(stockQuantity));
    }

    @Transient
    public BigDecimal getLastPurchaseUnitPriceWithVat() {
        if (lastPurchaseUnitPrice == null) return null;
        return lastPurchaseUnitPrice.multiply(BigDecimal.ONE.add(vatRate));
    }
    @Transient
    public BigDecimal getUnitProfit() {
        if (lastPurchaseUnitPrice == null) return null;
        return saleUnitPrice.subtract(getLastPurchaseUnitPriceWithVat());
    }

    @Transient
    public BigDecimal getTotalProfit() {
        if (getUnitProfit() == null) return null;
        return getUnitProfit().multiply(BigDecimal.valueOf(stockQuantity));
    }
}
