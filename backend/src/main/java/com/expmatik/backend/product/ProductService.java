package com.expmatik.backend.product;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.expmatik.backend.batch.DTOs.BatchValidationResponse;
import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.file.FileStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class ProductService {

    private static final String OpenFoodFactsApiUrl = "https://world.openfoodfacts.org/api/v3/product/";
    private static final long MIN_WAIT_INTERVAL = 1000; 
    private long lastCallTimestamp = 0;

    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public ProductService(ProductRepository productRepository, FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
    }
    @Transactional(readOnly = true)
    public Optional<Product> findByBarcodeOptional(UUID userId, String barcode) {
        return productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId);
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

    private synchronized void controlRateLimit() {
        long now = System.currentTimeMillis();
        long timeSinceLastCall = now - lastCallTimestamp;

        if (timeSinceLastCall < MIN_WAIT_INTERVAL) {
            try {
                // El hilo se duerme el tiempo que falta para cumplir el intervalo
                Thread.sleep(MIN_WAIT_INTERVAL - timeSinceLastCall);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Actualizamos el timestamp al momento actual (post-espera)
        lastCallTimestamp = System.currentTimeMillis();
    }

    @Transactional(readOnly = true)
    public Optional<Product> findProductInOpenFoodFacts(String barcode) {
        ObjectMapper mapper = new ObjectMapper();
        int maxAttempts = 1;
        int delay = 800;

        for (int i = 0; i < maxAttempts; i++) {
            controlRateLimit();
            try {
                JsonNode response = fetchOpenFoodFactsResponse(barcode, mapper);
                
                String status = response.path("status").asText();
                JsonNode productNode = response.path("product");
                int empty = productNode.path("empty").asInt(0);

                if ("success".equals(status) || "1".equals(status)) {
                    if (empty == 0 && !productNode.isMissingNode()) {
                        return Optional.of(mapJsonToProduct(response, barcode));
                    }
                }

                if (i == maxAttempts - 1) return Optional.empty();
                
                Thread.sleep(delay);

            } catch (IOException | InterruptedException e) {
                if (i == maxAttempts - 1) {
                    System.err.println("Final attempt failed for barcode " + barcode + ": " + e.getMessage());
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private Product mapJsonToProduct(JsonNode response, String barcode) {
        JsonNode pNode = response.path("product");
        
        String name = pNode.path("product_name_es").asText();
        if (name.isEmpty()) name = pNode.path("product_name").asText("Unknown Product");
        
        String brand = pNode.path("brands").asText();
        if (brand == null || brand.trim().isEmpty()) brand = "Unknown";
        
        String description = pNode.path("generic_name_es").asText(pNode.path("generic_name").asText(""));
        String quantity = pNode.path("quantity").asText("");
        String imageUrl = pNode.path("image_url").asText();
        
        Product product = new Product();
        product.setName((name + " " + quantity).trim());
        product.setBrand(brand.trim());
        product.setDescription(description);
        product.setImageUrl(imageUrl);
        product.setIsPerishable(true);
        product.setBarcode(barcode);
        product.setIsCustom(false);
        product.setCreatedBy(null);
        return product;
    }

    protected JsonNode fetchOpenFoodFactsResponse(String barcode, ObjectMapper mapper) throws IOException {
        String apiUrl = OpenFoodFactsApiUrl + barcode;
        URL url = URI.create(apiUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "PostmanRuntime/7.39.1");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Connection", "keep-alive");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");

        int responseCode = connection.getResponseCode();

        if (responseCode >= 200 && responseCode < 300) {
            return mapper.readTree(connection.getInputStream());
        } else {
            // OFF devuelve JSON de error (con status: failure) por el ErrorStream
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                return mapper.readTree(errorStream);
            }
            // Si no hay stream de error, devolvemos un nodo de fallo genérico
            return mapper.createObjectNode().put("status", "failure");
        }
    }

    @Transactional(readOnly = true)
    public void checkUniqueBarcode(String barcode, UUID userId) {
        if (productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId).isPresent()) {
            throw new ConflictException("A product with this barcode already exists.");
        }
    }

    @Transactional(readOnly = true)
    public Boolean existsByBarcode(String barcode,UUID userId) {
        
        if(productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId).isPresent()) {
            return true;
        }else if (findProductInOpenFoodFacts(barcode).isPresent()) {
            return true;
        }else {
            return false;
        } 
    }

    @Transactional(readOnly = true)
    public void checkUniqueBarcodeCustom(String barcode, UUID userId) {        
        
        if (productRepository.findByBarcodeAndIsCustomFalseOrBarcodeAndIsCustomTrueAndCreatedById(barcode, userId).isPresent()) {
            throw new ConflictException("A product with this barcode already exists.");
        }

        Optional<Product> openFoodFactsProduct = findProductInOpenFoodFacts(barcode);
        
        if (openFoodFactsProduct.isPresent()) {
            throw new ConflictException("A product with this barcode already exists in the external catalog.");
        }

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
        
        return save(product);
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
    public Page<Product> searchAllProducts(UUID userId, String name, String brand, String barcode, Pageable pageable) {
        String nameParam = (name != null && !name.isBlank()) ? name : null;
        String brandParam = (brand != null && !brand.isBlank()) ? brand : null;
        String barcodeParam = (barcode != null && !barcode.isBlank()) ? barcode : null;

        return productRepository.searchAdvanced(userId, nameParam, brandParam, barcodeParam, pageable);
    }

    @Transactional
    public Product createProductCustom(Product product,MultipartFile image) {
        checkUniqueBarcodeCustom(product.getBarcode(), product.getCreatedBy().getId());
        return updateProductImage(product, image, null);
    }

    @Transactional
    public Product createProductOpenFoodFacts(String barcode,UUID userId) {

        checkUniqueBarcode(barcode, userId);

        Optional<Product> openFoodFactsProduct = findProductInOpenFoodFacts(barcode);
        
        if (openFoodFactsProduct.isEmpty()) {
            throw new ResourceNotFoundException(
                "Product with barcode " + barcode + " not found in Open Food Facts external catalog. Consider creating it as a custom product."
            );
        }
        
        Product product = openFoodFactsProduct.get();
        product.setIsCustom(false);
        product = updateProductImage(product, null, product.getImageUrl());
        return product;
    }
    
    @Transactional(readOnly = true)
    public BatchValidationResponse validateBarcodes(List<String> barcodes, UUID userId) {
        List<String> valid = new ArrayList<>();
        List<String> notFound = new ArrayList<>();
        
        for (String barcode : barcodes) {
            Optional<Product> productInDb = findByBarcodeOptional(userId, barcode);
            
            if (productInDb.isPresent()) {
                valid.add(barcode);
            } else {
                Optional<Product> productInApi = findProductInOpenFoodFacts(barcode);
                
                if (productInApi.isPresent()) {
                    valid.add(barcode);
                } else {
                    notFound.add(barcode);
                }
            }
        }
        
        return BatchValidationResponse.of(valid, notFound);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchProductsWithoutProductInfoForUser(UUID userId, String name, String brand, String barcode, Pageable pageable) {
        String nameParam = (name != null && !name.isBlank()) ? name : null;
        String brandParam = (brand != null && !brand.isBlank()) ? brand : null;
        String barcodeParam = (barcode != null && !barcode.isBlank()) ? barcode : null;
        return productRepository.searchProductsWithoutProductInfoForUser(userId,nameParam,brandParam,barcodeParam,pageable);
    }
}
