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
import com.expmatik.backend.product.DTOs.ProductCreateCustom;


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
    public Product findByBarcode(UUID userId,String barcode) {
        return productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId)
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
        if (productRepository.findByBarcodeAndIsCustomFalse(barcode).isPresent()) {
            throw new ConflictException("A product with this barcode already exists.");
        }
    }

    public void checkUniqueBarcodeCustom(String barcode, UUID userId) {
        if (productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId).isPresent()) {
            throw new ConflictException("A product with this barcode already exists.");
        }
    }

    public void checkUniqueName(String name) {
        if (productRepository.findByNameAndIsCustomFalse(name).isPresent()) {
            throw new ConflictException("A product with this name already exists.");
        }
    }

    public void checkUniqueNameCustom(String name, UUID userId) {
        if (productRepository.findByNameAndIsCustomFalseOrNameAndIsCustomTrueAndCreatedById(name, userId).isPresent()) {
            throw new ConflictException("A product with this name already exists.");
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

    @Transactional(readOnly = true)
    public List<Product> searchAllProducts(UUID userId, String name, String brand, String barcode) {
        List<Product> results = new java.util.ArrayList<>();
        
        // Si se proporciona código de barras, búsqueda exacta (catálogo solo)
        if (barcode != null && !barcode.isBlank()) {
            var product = productRepository.findByBarcodeAndIsCustomFalse(barcode);
            if (product.isPresent()) {
                results.add(product.get());
            }
            return results;
        }
        
        // Si se proporcionan nombre y marca
        if (name != null && !name.isBlank() && brand != null && !brand.isBlank()) {
            results.addAll(productRepository.findByNameContainingIgnoreCaseAndBrandContainingIgnoreCaseAndIsCustomFalse(name, brand));
            results.addAll(productRepository.findByIsCustomTrueAndNameContainingIgnoreCaseAndBrandContainingIgnoreCaseAndCreatedById(name, brand, userId));
            return results;
        }
        
        // Si solo se proporciona nombre
        if (name != null && !name.isBlank()) {
            results.addAll(productRepository.findByNameContainingIgnoreCaseAndIsCustomFalse(name));
            results.addAll(productRepository.findByIsCustomTrueAndNameContainingIgnoreCaseAndCreatedById(name, userId));
            return results;
        }
        
        // Si solo se proporciona marca
        if (brand != null && !brand.isBlank()) {
            results.addAll(productRepository.findByBrandContainingIgnoreCaseAndIsCustomFalse(brand));
            results.addAll(productRepository.findByIsCustomTrueAndBrandContainingIgnoreCaseAndCreatedById(brand, userId));
            return results;
        }
        
        // Si no hay filtros, devuelve catálogo + todos mis productos personalizados
        results.addAll(productRepository.findByIsCustomFalse());
        results.addAll(productRepository.findByIsCustomTrueAndCreatedById(userId));
        return results;
    }


    @Transactional
    public Product createProduct(UUID userId, ProductCreate productDTO,String imageUrl) {
        Product product = productDTO.toEntity();
        checkUniqueBarcode(product.getBarcode());
        checkUniqueName(product.getName());
        return updateProductImage(product, null, imageUrl);
    }

    @Transactional
    public Product createProductCustom(UUID userId, ProductCreateCustom productDTO,MultipartFile image) {
        Product product = productDTO.toEntity();
        checkUniqueBarcodeCustom(product.getBarcode(), userId);
        checkUniqueNameCustom(product.getName(), userId);
        return updateProductImage(product, image, null);
    }
    
}
