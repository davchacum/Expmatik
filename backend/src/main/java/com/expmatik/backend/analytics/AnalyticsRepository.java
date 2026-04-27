package com.expmatik.backend.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.expmatik.backend.invoice.InvoiceStatus;
import com.expmatik.backend.sale.Sale;
import com.expmatik.backend.sale.TransactionStatus;

public interface AnalyticsRepository extends JpaRepository<Sale, UUID> {

    @Query("""
        SELECT SUM(b.unitPrice * b.quantity) FROM Batch b
        WHERE b.invoice.user.id = :userId
        AND (b.invoice.invoiceDate >= COALESCE(:startDate, b.invoice.invoiceDate))
        AND (b.invoice.invoiceDate <= COALESCE(:endDate, b.invoice.invoiceDate))
        AND b.invoice.status = :status
    """)
    BigDecimal getExpensesTotal(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("status") InvoiceStatus status
    );

    @Query(value = """
        SELECT b.product.name, SUM(b.unitPrice * b.quantity), SUM(b.quantity) FROM Batch b
        WHERE b.invoice.user.id = :userId
        AND (b.invoice.invoiceDate >= COALESCE(:startDate, b.invoice.invoiceDate))
        AND (b.invoice.invoiceDate <= COALESCE(:endDate, b.invoice.invoiceDate))
        AND b.invoice.status = :status
        AND (:productName IS NULL OR LOWER(b.product.name) LIKE LOWER(CONCAT('%', CAST(:productName AS string), '%')))
        AND (:brand IS NULL OR LOWER(b.product.brand) LIKE LOWER(CONCAT('%', CAST(:brand AS string), '%')))
        GROUP BY b.product.id, b.product.name
        """,
        countQuery = """
        SELECT COUNT(DISTINCT b.product.id) FROM Batch b
        WHERE b.invoice.user.id = :userId
        AND (b.invoice.invoiceDate >= COALESCE(:startDate, b.invoice.invoiceDate))
        AND (b.invoice.invoiceDate <= COALESCE(:endDate, b.invoice.invoiceDate))
        AND b.invoice.status = :status
        AND (:productName IS NULL OR LOWER(b.product.name) LIKE LOWER(CONCAT('%', CAST(:productName AS string), '%')))
        AND (:brand IS NULL OR LOWER(b.product.brand) LIKE LOWER(CONCAT('%', CAST(:brand AS string), '%')))
        """)
    Page<Object[]> getExpensesBreakdownByProduct(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("status") InvoiceStatus status,
        @Param("productName") String productName,
        @Param("brand") String brand,
        Pageable pageable
    );

    @Query("""
        SELECT SUM(s.totalAmount) FROM Sale s
        WHERE s.vendingSlot.vendingMachine.user.id = :userId
        AND (s.saleDate >= COALESCE(:startDate, s.saleDate))
        AND (s.saleDate <= COALESCE(:endDate, s.saleDate))
        AND s.status = :status
        AND (:machineId IS NULL OR s.vendingSlot.vendingMachine.id = :machineId)
    """)
    BigDecimal getIncomeTotal(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("status") TransactionStatus status,
        @Param("machineId") UUID machineId
    );

    @Query(value = """
        SELECT s.vendingSlot.vendingMachine.name, SUM(s.totalAmount), COUNT(s) FROM Sale s
        WHERE s.vendingSlot.vendingMachine.user.id = :userId
        AND (s.saleDate >= COALESCE(:startDate, s.saleDate))
        AND (s.saleDate <= COALESCE(:endDate, s.saleDate))
        AND s.status = :status
        AND (:machineId IS NULL OR s.vendingSlot.vendingMachine.id = :machineId)
        GROUP BY s.vendingSlot.vendingMachine.id, s.vendingSlot.vendingMachine.name
        """,
        countQuery = """
        SELECT COUNT(DISTINCT s.vendingSlot.vendingMachine.id) FROM Sale s
        WHERE s.vendingSlot.vendingMachine.user.id = :userId
        AND (s.saleDate >= COALESCE(:startDate, s.saleDate))
        AND (s.saleDate <= COALESCE(:endDate, s.saleDate))
        AND s.status = :status
        AND (:machineId IS NULL OR s.vendingSlot.vendingMachine.id = :machineId)
        """)
    Page<Object[]> getIncomeBreakdownByMachine(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("status") TransactionStatus status,
        @Param("machineId") UUID machineId,
        Pageable pageable
    );

    @Query(value = """
        SELECT s.product.name, SUM(s.totalAmount), COUNT(s) FROM Sale s
        WHERE s.vendingSlot.vendingMachine.user.id = :userId
        AND (s.saleDate >= COALESCE(:startDate, s.saleDate))
        AND (s.saleDate <= COALESCE(:endDate, s.saleDate))
        AND s.status = :status
        AND (:machineId IS NULL OR s.vendingSlot.vendingMachine.id = :machineId)
        AND (:productName IS NULL OR LOWER(s.product.name) LIKE LOWER(CONCAT('%', CAST(:productName AS string), '%')))
        AND (:brand IS NULL OR LOWER(s.product.brand) LIKE LOWER(CONCAT('%', CAST(:brand AS string), '%')))
        GROUP BY s.product.id, s.product.name
        """,
        countQuery = """
        SELECT COUNT(DISTINCT s.product.id) FROM Sale s
        WHERE s.vendingSlot.vendingMachine.user.id = :userId
        AND (s.saleDate >= COALESCE(:startDate, s.saleDate))
        AND (s.saleDate <= COALESCE(:endDate, s.saleDate))
        AND s.status = :status
        AND (:machineId IS NULL OR s.vendingSlot.vendingMachine.id = :machineId)
        AND (:productName IS NULL OR LOWER(s.product.name) LIKE LOWER(CONCAT('%', CAST(:productName AS string), '%')))
        AND (:brand IS NULL OR LOWER(s.product.brand) LIKE LOWER(CONCAT('%', CAST(:brand AS string), '%')))
        """)
    Page<Object[]> getIncomeBreakdownByProduct(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("status") TransactionStatus status,
        @Param("machineId") UUID machineId,
        @Param("productName") String productName,
        @Param("brand") String brand,
        Pageable pageable
    );

}
