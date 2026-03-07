package com.expmatik.backend.productInfo;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
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

    @Transactional(readOnly = true)
    public ProductInfo findByProductIdAndUserId(UUID productId, UUID userId) {
        return productInfoRepository.findByProductIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductInfo not found for product id: " + productId + " and user id: " + userId));
    }

    @Transactional
    public ProductInfo getOrCreateProductInfo(UUID productId, User user) {

        Product product = productService.findById(productId);
        if(product.getIsCustom() && !product.getCreatedBy().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("You are not authorized to view this product info.");
        }
        Optional<ProductInfo> optionalProductInfo = productInfoRepository.findByProductIdAndUserId(productId, user.getId());
        if(optionalProductInfo.isPresent()) {
            return optionalProductInfo.get();
        }
        ProductInfo productInfo = createProductInfoForProduct(product, user);
        return productInfo;
    }

    @Transactional
    public ProductInfo createProductInfoForProduct(Product product, User user) {
        ProductInfo productInfo = new ProductInfo();
        productInfo.setStockQuantity(0);
        productInfo.setUnitPrice(BigDecimal.ONE);
        productInfo.setVatRate(new BigDecimal("0.21"));
        productInfo.setProduct(product);
        productInfo.setUser(user);
        return productInfoRepository.save(productInfo);
    }

    @Transactional
    public ProductInfo updateProductInfo(UUID productInfoId, User user, ProductInfoUpdate updatedInfo) {

        ProductInfo existingInfo = findById(productInfoId);
        if(!existingInfo.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("You are not authorized to update this product info.");
        }
        existingInfo.setStockQuantity(updatedInfo.stockQuantity());
        existingInfo.setUnitPrice(updatedInfo.unitPrice());
        existingInfo.setVatRate(updatedInfo.vatRate());
        return productInfoRepository.save(existingInfo);
    }

    @Transactional
    public ProductInfo addStockQuantity(UUID productInfoId, User user, Integer newStockQuantity, BigDecimal lastPurchaseUnitPrice) {
        ProductInfo existingInfo = findById(productInfoId);
        if(!existingInfo.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("You are not authorized to update this product info.");
        }
        if(newStockQuantity == null || newStockQuantity < 0) {
            throw new ConflictException("New stock quantity must be non-negative.");
        }
        existingInfo.setStockQuantity(existingInfo.getStockQuantity() + newStockQuantity);
        return productInfoRepository.save(existingInfo);
    }
}
