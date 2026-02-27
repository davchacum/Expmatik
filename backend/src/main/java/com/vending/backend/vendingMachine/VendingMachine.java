package com.vending.backend.vendingMachine;

import com.vending.backend.model.BaseEntity;
import com.vending.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class VendingMachine extends BaseEntity{

    @NotBlank
    @Size(max = 100)
    @Column(name = "location",nullable = false, length = 100)
    private String location;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name",nullable = false, unique = true, length = 100)
    private String name;

    @NotNull
    @Positive
    @Column(name = "column_count",nullable = false,updatable = false)
    private Integer columnCount;

    @NotNull
    @Positive
    @Column(name = "row_count",nullable = false,updatable = false)
    private Integer rowCount;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}
