package com.expmatik.backend.sale;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    Optional<Sale> findById(UUID id);

    @Query("SELECT s FROM Sale s WHERE " +
        "(s.vendingSlot.vendingMachine.user.id = :userId) " +
        "AND (:barcode IS NULL OR LOWER(s.product.barcode) LIKE LOWER(CONCAT('%', CAST(:barcode AS string), '%'))) " +
        "AND (:machineId IS NULL OR s.vendingSlot.vendingMachine.id = :machineId) " +
        "AND (:slotId IS NULL OR s.vendingSlot.id = :slotId) " +
        "AND (:paymentMethod IS NULL OR s.paymentMethod = :paymentMethod) " +
        "AND (:status IS NULL OR s.status = :status) " +
        "AND (s.saleDate >= COALESCE(:startDate, s.saleDate)) " +
        "AND (s.saleDate <= COALESCE(:endDate, s.saleDate))"
    )
    Page<Sale> searchAdvanced(
        @Param("userId") UUID userId,
        @Param("barcode") String barcode,
        @Param("machineId") UUID machineId,
        @Param("slotId") UUID slotId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("paymentMethod") PaymentMethod paymentMethod,
        @Param("status") TransactionStatus status,
        Pageable pageable
    );
}
