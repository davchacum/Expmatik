package com.expmatik.backend.productInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductInfoRepository extends JpaRepository<ProductInfo, UUID> {

    Optional<ProductInfo> findByProductIdAndUserId(UUID productId, UUID userId);

    @Query("SELECT pi FROM ProductInfo pi WHERE pi.user.id = :userId ORDER BY pi.stockQuantity DESC")
    List<ProductInfo> findAllByUserIdOrderByStockQuantityDesc(UUID userId);

}
