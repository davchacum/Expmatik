package com.expmatik.backend.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findById(UUID id);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByUserId(UUID userId);

    List<Invoice> findByUserIdAndStatus(UUID userId, InvoiceStatus status);

    @Query(value = "SELECT DISTINCT i.* FROM invoice i " +
           "LEFT JOIN batch b ON i.id = b.invoice_id " +
           "LEFT JOIN supplier s ON i.supplier_id = s.id " +
           "WHERE i.user_id = CAST(:userId AS uuid) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR i.invoice_date >= CAST(:startDate AS timestamp)) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR i.invoice_date <= CAST(:endDate AS timestamp)) " +
           "AND (:invoiceNumber IS NULL OR LOWER(i.invoice_number) LIKE LOWER(CONCAT('%', :invoiceNumber, '%'))) " +
           "AND (:supplierName IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :supplierName, '%'))) " +
           "GROUP BY i.id, i.invoice_date, i.invoice_number, i.status, i.supplier_id, i.user_id " +
           "HAVING (:minPrice IS NULL OR COALESCE(SUM(b.unit_price * b.quantity), 0) >= :minPrice) " +
           "AND (:maxPrice IS NULL OR COALESCE(SUM(b.unit_price * b.quantity), 0) <= :maxPrice)", 
           nativeQuery = true)
    List<Invoice> searchInvoices(
        @Param("userId") UUID userId,
        @Param("status") String status,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("invoiceNumber") String invoiceNumber,
        @Param("supplierName") String supplierName,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice
    );

}
