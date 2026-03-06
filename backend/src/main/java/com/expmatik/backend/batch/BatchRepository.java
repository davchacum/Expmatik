package com.expmatik.backend.batch;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BatchRepository extends JpaRepository<Batch, UUID> {

    @Query("SELECT b FROM Invoice i JOIN i.batch b WHERE i.id = :invoiceId AND b.id = :batchId")
    Optional<Batch> findBatchByBatchIdAndInvoiceId(UUID invoiceId, UUID batchId);
}
