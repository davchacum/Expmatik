package com.expmatik.backend.product;

import com.expmatik.backend.model.BaseEntity;
import com.expmatik.backend.user.User;

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
    @Column(name = "name",nullable = false, unique = true,length = 100)
    @Size(max = 100)
    private String name;

    @NotBlank
    @Column(name = "brand",nullable = false,length = 100)
    @Size(max = 100)
    private String brand;

    @Column(name = "description",length = 1000)
    @Size(max = 1000,min = 0)
    private String description = "";

    @Column(name = "image_url",length = 1000)
    @Size(max = 1000)
    private String imageUrl = "";

    @NotNull
    @Column(name = "is_perishable",nullable = false)
    private Boolean isPerishable;

    @NotBlank
    @Column(name = "barcode",nullable = false, unique = true,length = 20)
    @Size(max = 20)
    private String barcode;

    @NotNull
    @Column(name = "is_custom",nullable = false, updatable = false)
    private Boolean isCustom;

    @ManyToOne
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

}
