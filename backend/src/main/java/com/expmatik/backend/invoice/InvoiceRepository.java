package com.expmatik.backend.invoice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findById(UUID id);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByUserId(UUID userId);

    List<Invoice> findByUserIdAndStatus(UUID userId, InvoiceStatus status);

}
