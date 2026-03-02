package com.expmatik.backend.productInfo;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.ResourceNotFoundException;

@Service
public class ProductInfoService {

    private final ProductInfoRepository productInfoRepository;


    @Autowired
    public ProductInfoService(ProductInfoRepository productInfoRepository) {
        this.productInfoRepository = productInfoRepository;
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
    public ProductInfo findByProductId(UUID productId) {
        return productInfoRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductInfo not found for product id: " + productId));
    }





}
