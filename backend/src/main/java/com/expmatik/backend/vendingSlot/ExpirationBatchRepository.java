package com.expmatik.backend.vendingSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExpirationBatchRepository extends JpaRepository<ExpirationBatch, UUID> {
    
    @Query("SELECT eb FROM ExpirationBatch eb WHERE eb.vendingSlot.id = :slotId ORDER BY eb.expirationDate ASC")
    List<ExpirationBatch> findAllByVendingSlotIdOrderByExpirationDateAsc(UUID slotId);

    List<ExpirationBatch> findAllByExpirationDate(LocalDate expirationDate);

}
