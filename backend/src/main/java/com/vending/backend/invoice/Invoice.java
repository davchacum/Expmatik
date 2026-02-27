package com.vending.backend.invoice;

import java.time.LocalDateTime;
import java.util.List;

import com.vending.backend.batch.Batch;
import com.vending.backend.model.BaseEntity;
import com.vending.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Invoice extends BaseEntity {

    @NotNull
    @Column(name="invoice_date",nullable = false)
    private LocalDateTime invoiceDate;

    @NotBlank
    @Column(name="invoice_number",nullable = false, unique = true)
    private String invoiceNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name="status",nullable = false)
    private InvoiceStatus status;

    @NotNull
    @ManyToOne
    @JoinColumn(name="supplier_id",nullable = false)
    private Supplier supplier;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @OneToMany
    @JoinColumn(name = "invoice_id", nullable = false)
    private List<Batch> batch;

}
