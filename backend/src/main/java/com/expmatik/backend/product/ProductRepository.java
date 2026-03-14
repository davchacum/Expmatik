package com.expmatik.backend.product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    @Query("SELECT p FROM Product p WHERE (p.barcode = :barcode AND p.isCustom = false) OR (p.barcode = :barcode AND p.isCustom = true AND p.createdBy.id = :userId)")
    Optional<Product> findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(String barcode, UUID userId);

    @Query("SELECT p FROM Product p WHERE (p.name = :name AND p.isCustom = false) OR (p.name = :name AND p.isCustom = true AND p.createdBy.id = :userId)")
    Optional<Product> findByNameAndIsCustomFalseOrNameAndIsCustomTrueAndCreatedById(String name, UUID userId);

    List<Product> findByIsCustomTrueAndCreatedById(UUID userId);

    List<Product> findByIsCustomFalse();

    Optional<Product> findByBarcodeAndIsCustomFalse(String barcode);
    
    @Query("SELECT p FROM Product p WHERE " +
        "(p.isCustom = false OR (p.isCustom = true AND p.createdBy.id = :userId)) " +
        "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
        "AND (:brand IS NULL OR LOWER(p.brand) LIKE LOWER(CONCAT('%', CAST(:brand AS string), '%'))) " +
        "AND (:barcode IS NULL OR p.barcode = :barcode)")
    Page<Product> searchAdvanced(
        @Param("userId") UUID userId, 
        @Param("name") String name, 
        @Param("brand") String brand, 
        @Param("barcode") String barcode, 
        Pageable pageable
    );
}
