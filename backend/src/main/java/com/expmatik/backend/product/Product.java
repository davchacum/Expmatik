package com.expmatik.backend.product;

import com.expmatik.backend.model.BaseEntity;
import com.expmatik.backend.user.User;
import com.expmatik.backend.validation.ValidBarcode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Product extends BaseEntity{

    @NotBlank
    @Column(name = "name",nullable = false,length = 100)
    @Size(max = 100)
    private String name;

    @NotBlank
    @Column(name = "brand",nullable = false,length = 100)
    @Size(max = 100)
    private String brand;

    @Column(name = "description",length = 1000)
    @Size(max = 1000,min = 0)
    private String description;

    @NotNull
    @Column(name = "image_url",length = 1000, nullable = false)
    @Size(max = 1000)
    private String imageUrl;

    @NotNull
    @Column(name = "is_perishable",nullable = false)
    private Boolean isPerishable;

    @NotBlank
    @ValidBarcode
    @Column(name = "barcode",nullable = false,length = 13)
    private String barcode;

    @NotNull
    @Column(name = "is_custom",nullable = false, updatable = false)
    private Boolean isCustom;

    @ManyToOne
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

}
