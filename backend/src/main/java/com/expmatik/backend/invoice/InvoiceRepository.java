package com.expmatik.backend.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findById(UUID id);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByUserIdAndStatus(UUID userId, InvoiceStatus status);

    @Query("SELECT i FROM Invoice i " +
           "WHERE i.user.id = :userId " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:startDate IS NULL OR i.invoiceDate >= :startDate) " +
           "AND (:endDate IS NULL OR i.invoiceDate <= :endDate) " +
           "AND (:invoiceNumber IS NULL OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', CAST(:invoiceNumber AS string), '%'))) " +
           "AND (:supplierName IS NULL OR LOWER(i.supplier.name) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%'))) " +
           "AND (:minPrice IS NULL OR (SELECT COALESCE(SUM(b.unitPrice * b.quantity), 0) FROM Batch b WHERE b.invoice = i) >= :minPrice) " +
           "AND (:maxPrice IS NULL OR (SELECT COALESCE(SUM(b.unitPrice * b.quantity), 0) FROM Batch b WHERE b.invoice = i) <= :maxPrice)")
    Page<Invoice> searchInvoices(
        @Param("userId") UUID userId,
        @Param("status") InvoiceStatus status,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("invoiceNumber") String invoiceNumber,
        @Param("supplierName") String supplierName,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );

}
