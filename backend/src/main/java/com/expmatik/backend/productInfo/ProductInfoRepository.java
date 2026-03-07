package com.expmatik.backend.productInfo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductInfoRepository extends JpaRepository<ProductInfo, UUID> {

    Optional<ProductInfo> findByProductIdAndUserId(UUID productId, UUID userId);

}
