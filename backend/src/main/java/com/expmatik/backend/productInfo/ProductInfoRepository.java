package com.expmatik.backend.productInfo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductInfoRepository extends JpaRepository<ProductInfo, UUID> {

    Optional<ProductInfo> findByProductIdAndUserId(UUID productId, UUID userId);

    @Query("SELECT pi FROM ProductInfo pi WHERE pi.user.id = :userId ORDER BY pi.stockQuantity DESC")
    Page<ProductInfo> findAllByUserIdOrderByStockQuantityDesc(UUID userId,Pageable pageable);

}
