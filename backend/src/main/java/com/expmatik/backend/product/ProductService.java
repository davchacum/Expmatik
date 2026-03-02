package com.expmatik.backend.product;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.file.FileStorageService;
import com.expmatik.backend.product.DTOs.ProductCreate;


@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public ProductService(ProductRepository productRepository, FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public Product findByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with barcode: " + barcode));
    }

    @Transactional(readOnly = true)
    public Product findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }

    public void checkUniqueBarcode(String barcode) {
        if (productRepository.findByBarcode(barcode).isPresent()) {
            throw new ConflictException("A product with this barcode already exists.");
        }
    }
    
    @Transactional
    public String storeCustomProductImage(MultipartFile image) {
        return fileStorageService.saveCustomProductImage(image);
    }

    @Transactional
    public void deleteProductImage(String imageUrl) {
        fileStorageService.deleteProductImage(imageUrl);
    }

    @Transactional
    public Product updateProductImage(Product product, MultipartFile file, String imageUrl) {
        // Si es producto personalizado, requiere archivo
        if (product.getIsCustom()) {
            if (file == null || file.isEmpty()) {
                throw new BadRequestException("Custom products require an image file");
            }
            
            // Guardar archivo personalizado
            String newImageUrl = fileStorageService.saveCustomProductImage(file);
            
            // Eliminar imagen anterior si existía
            if (product.getImageUrl() != null && !fileStorageService.isExternalUrl(product.getImageUrl())) {
                fileStorageService.deleteProductImage(product.getImageUrl());
            }
            
            product.setImageUrl(newImageUrl);
        } 
        // Si no es personalizado, requiere URL
        else {
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new BadRequestException("Non-custom products require an image URL");
            }
            
            // Eliminar archivo anterior si era local
            if (product.getImageUrl() != null && !fileStorageService.isExternalUrl(product.getImageUrl())) {
                fileStorageService.deleteProductImage(product.getImageUrl());
            }
            
            product.setImageUrl(imageUrl);
        }
        
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> getCustomProductsByUserId(UUID userId) {
        return productRepository.findByIsCustomTrueAndCreatedById(userId);
    }

    @Transactional(readOnly = true)
    public List<Product> getAllNonCustomProducts() {
        return productRepository.findByIsCustomFalse();
    }

    @Transactional
    public Product createProduct(ProductCreate productDTO,MultipartFile image,String imageUrl) {
        Product product = productDTO.toEntity();
        checkUniqueBarcode(product.getBarcode());
        return updateProductImage(product, image, imageUrl);
    }
    
}
