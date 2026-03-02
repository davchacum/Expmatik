package com.expmatik.backend.product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    Optional<Product> findByBarcode(String barcode);

    List<Product> findByIsCustomTrueAndCreatedById(UUID userId);

    List<Product> findByIsCustomFalse();
}
