package com.expmatik.backend.sale;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    Optional<Sale> findById(UUID id);
    
    @Query("SELECT s.product.barcode, YEAR(s.saleDate), MONTH(s.saleDate), COUNT(s) " +
           "FROM Sale s WHERE s.status = :status AND s.product.barcode = :barcode AND s.vendingSlot.vendingMachine.user.id = :userId " +
           "GROUP BY s.product.barcode, YEAR(s.saleDate), MONTH(s.saleDate)")
    List<Object[]> findMonthlySalesByBarcode(@Param("status") TransactionStatus status, @Param("barcode") String barcode, @Param("userId") UUID userId);

    @Query("SELECT s FROM Sale s WHERE " +
        "(s.vendingSlot.vendingMachine.user.id = :userId) " +
        "AND (:barcode IS NULL OR LOWER(s.product.barcode) LIKE LOWER(CONCAT('%', CAST(:barcode AS string), '%'))) " +
        "AND (:machineName IS NULL OR LOWER(s.vendingSlot.vendingMachine.name) LIKE LOWER(CONCAT('%', CAST(:machineName AS string), '%'))) " +
        "AND (:rowNumber IS NULL OR s.vendingSlot.rowNumber = :rowNumber) " +
        "AND (:columnNumber IS NULL OR s.vendingSlot.columnNumber = :columnNumber) " +
        "AND (:paymentMethod IS NULL OR s.paymentMethod = :paymentMethod) " +
        "AND (:status IS NULL OR s.status = :status) " +
        "AND (s.saleDate >= COALESCE(:startDate, s.saleDate)) " +
        "AND (s.saleDate <= COALESCE(:endDate, s.saleDate))"
    )
    List<Sale> searchAdvanced(
        @Param("userId") UUID userId,
        @Param("barcode") String barcode,
        @Param("machineName") String machineName,
        @Param("rowNumber") Integer rowNumber,
        @Param("columnNumber") Integer columnNumber,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("paymentMethod") PaymentMethod paymentMethod,
        @Param("status") TransactionStatus status
    );

    @Query("SELECT s FROM Sale s WHERE " +
        "(s.vendingSlot.vendingMachine.user.id = :userId) " +
        "AND (:barcode IS NULL OR LOWER(s.product.barcode) LIKE LOWER(CONCAT('%', CAST(:barcode AS string), '%'))) " +
        "AND (:machineName IS NULL OR LOWER(s.vendingSlot.vendingMachine.name) LIKE LOWER(CONCAT('%', CAST(:machineName AS string), '%'))) " +
        "AND (:rowNumber IS NULL OR s.vendingSlot.rowNumber = :rowNumber) " +
        "AND (:columnNumber IS NULL OR s.vendingSlot.columnNumber = :columnNumber) " +
        "AND (:paymentMethod IS NULL OR s.paymentMethod = :paymentMethod) " +
        "AND (:status IS NULL OR s.status = :status) " +
        "AND (s.saleDate >= COALESCE(:startDate, s.saleDate)) " +
        "AND (s.saleDate <= COALESCE(:endDate, s.saleDate))"
    )
    Page<Sale> searchAdvanced(
        @Param("userId") UUID userId,
        @Param("barcode") String barcode,
        @Param("machineName") String machineName,
        @Param("rowNumber") Integer rowNumber,
        @Param("columnNumber") Integer columnNumber,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("paymentMethod") PaymentMethod paymentMethod,
        @Param("status") TransactionStatus status,
        Pageable pageable
    );
}
