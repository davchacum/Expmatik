package com.expmatik.backend.sale;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    Optional<Sale> findById(UUID id);
}
