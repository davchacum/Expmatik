package com.expmatik.backend.invoice;

import com.expmatik.backend.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Supplier extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

}
