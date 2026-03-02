package com.expmatik.backend.product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    @Query("SELECT p FROM Product p WHERE (p.barcode = :barcode AND p.isCustom = false) OR (p.barcode = :barcode AND p.isCustom = true AND p.createdBy.id = :userId)")
    Optional<Product> findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(String barcode, UUID userId);

    @Query("SELECT p FROM Product p WHERE (p.name = :name AND p.isCustom = false) OR (p.name = :name AND p.isCustom = true AND p.createdBy.id = :userId)")
    Optional<Product> findByNameAndIsCustomFalseOrNameAndIsCustomTrueAndCreatedById(String name, UUID userId);

    List<Product> findByIsCustomTrueAndCreatedById(UUID userId);

    List<Product> findByIsCustomFalse();

    List<Product> findByNameContainingIgnoreCaseAndIsCustomFalse(String name);

    Optional<Product> findByBarcodeAndIsCustomFalse(String barcode);

    Optional<Product> findByNameAndIsCustomFalse(String name);

    List<Product> findByBrandContainingIgnoreCaseAndIsCustomFalse(String brand);

    List<Product> findByNameContainingIgnoreCaseAndBrandContainingIgnoreCaseAndIsCustomFalse(String name, String brand);

    List<Product> findByIsCustomTrueAndNameContainingIgnoreCaseAndCreatedById(String name, UUID userId);

    List<Product> findByIsCustomTrueAndBrandContainingIgnoreCaseAndCreatedById(String brand, UUID userId);

    List<Product> findByIsCustomTrueAndNameContainingIgnoreCaseAndBrandContainingIgnoreCaseAndCreatedById(String name, String brand, UUID userId);
    
}
