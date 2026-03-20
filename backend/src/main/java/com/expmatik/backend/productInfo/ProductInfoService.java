package com.expmatik.backend.productInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.exceptions.UnauthorizedActionException;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.product.ProductService;
import com.expmatik.backend.productInfo.DTOs.ProductInfoUpdate;
import com.expmatik.backend.user.User;

@Service
public class ProductInfoService {

    private final ProductInfoRepository productInfoRepository;
    private final ProductService productService;


    @Autowired
    public ProductInfoService(ProductInfoRepository productInfoRepository, ProductService productService) {
        this.productInfoRepository = productInfoRepository;
        this.productService = productService;
    }

    @Transactional
    public ProductInfo save(ProductInfo productInfo) {
        return productInfoRepository.save(productInfo);
    }

    @Transactional(readOnly = true)
    public ProductInfo findById(UUID id) {
        return productInfoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductInfo not found with id: " + id));
    }

    @Transactional
    public ProductInfo getOrCreateProductInfo(UUID productId, User user, BigDecimal unitPrice) {

        Product product = productService.findById(productId);
        if(product.getIsCustom() && !product.getCreatedBy().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("You are not authorized to view this product info.");
        }
        Optional<ProductInfo> optionalProductInfo = productInfoRepository.findByProductIdAndUserId(productId, user.getId());
        if(optionalProductInfo.isPresent()) {
            return optionalProductInfo.get();
        }

        ProductInfo productInfo = createProductInfoForProduct(product, user, unitPrice);
        return productInfo;
    }

    @Transactional
    public ProductInfo createProductInfoForProduct(Product product, User user, BigDecimal unitPrice) {
        if(unitPrice == null) {
            unitPrice = BigDecimal.ONE;
        }
        BigDecimal vatRate = new BigDecimal("0.21");
        ProductInfo productInfo = new ProductInfo();
        productInfo.setStockQuantity(0);
        productInfo.setVatRate(vatRate);
        BigDecimal saleUnitPrice = unitPrice.multiply(BigDecimal.ONE.add(vatRate)).setScale(2, RoundingMode.HALF_UP);
        productInfo.setSaleUnitPrice(saleUnitPrice.multiply(new BigDecimal("1.2")).setScale(2, RoundingMode.HALF_UP));
        productInfo.setProduct(product);
        productInfo.setUser(user);
        productInfo.setNeedUpdate(true);   
        return save(productInfo);
    }

    @Transactional
    public ProductInfo updateProductInfo(UUID productInfoId, User user, ProductInfoUpdate updatedInfo) {

        ProductInfo existingInfo = findById(productInfoId);
        if(!existingInfo.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("You are not authorized to update this product info.");
        }
        existingInfo.setStockQuantity(updatedInfo.stockQuantity());
        existingInfo.setSaleUnitPrice(updatedInfo.saleUnitPrice());
        existingInfo.setVatRate(updatedInfo.vatRate());
        existingInfo.setNeedUpdate(false);
        return save(existingInfo);
    }

    @Transactional
    public ProductInfo editStockQuantity(UUID productInfoId, User user, Integer newStockQuantity, BigDecimal lastPurchaseUnitPrice) {
        ProductInfo existingInfo = findById(productInfoId);
        if(!existingInfo.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("You are not authorized to update this product info.");
        }
        Integer updatedStockQuantity = existingInfo.getStockQuantity() + newStockQuantity;
        if(updatedStockQuantity < 0) {
            updatedStockQuantity = 0;
        }
        existingInfo.setStockQuantity(updatedStockQuantity);
        if(lastPurchaseUnitPrice != null) {
            existingInfo.setLastPurchaseUnitPrice(lastPurchaseUnitPrice);
        }
        return save(existingInfo);
    }

    @Transactional(readOnly = true)
    public Page<ProductInfo> findAllByUserIdOrderByStockQuantityDesc(UUID userId, Pageable pageable) {
        return productInfoRepository.findAllByUserIdOrderByStockQuantityDesc(userId, pageable);
    }
}
