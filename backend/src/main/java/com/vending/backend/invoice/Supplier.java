package com.vending.backend.invoice;

import com.vending.backend.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Supplier extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

}
