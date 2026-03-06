package com.expmatik.backend.product;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.file.FileStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class ProductService {

    private static final String OpenFoodFactsApiUrl = "https://world.openfoodfacts.org/api/v3/product/";
    private static final long MIN_WAIT_INTERVAL = 700; 
    private long lastCallTimestamp = 0;

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
    Optional<Product> findProductInOpenFoodFacts(String barcode) {
        controlRateLimit(); // Controlar el rate limit antes de hacer la llamada
        
        ObjectMapper mapper = new ObjectMapper();
        try {
            String apiUrl = OpenFoodFactsApiUrl + barcode;
            URL url = URI.create(apiUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Expmatik - Universidad de Sevilla - TFG - (davchacum@alum.us.es)");

            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            
            JsonNode response = mapper.readTree(connection.getInputStream());
            String status = response.path("status").asText();
            
            // Si el producto no fue encontrado, devolver Optional vacío
            if ("failure".equals(status) || "product_not_found".equals(response.path("result").path("id").asText())) {
                return Optional.empty();
            }
                    if (response.isEmpty()) {
            throw new ResourceNotFoundException("Product not found with barcode: " + barcode);
            }
        
            String name = response.path("product").path("product_name_es").asText();
            String brand = response.path("product").path("brands").asText();
            String description = response.path("product").path("generic_name_es").asText();
            String quantity = response.path("product").path("quantity").asText();
            name = name + " " + quantity;
            String imageUrl = response.path("product").path("image_url").asText();
            
            Product product = new Product();
            product.setName(name);
            product.setBrand(brand);
            product.setDescription(description);
            product.setImageUrl(imageUrl);
            product.setIsPerishable(true);
            product.setBarcode(barcode);
            product.setIsCustom(false);
            product.setCreatedBy(null);
            product.setImageUrl(imageUrl);
            return Optional.of(product);
        } catch (IOException e) {
            // Si hay error de conexión, devolver Optional vacío
            System.err.println("Error connecting to Open Food Facts API: " + e.getMessage());
            return Optional.empty();
        }
    }

    public void checkUniqueBarcode(String barcode) {
        if (productRepository.findByBarcodeAndIsCustomFalse(barcode).isPresent()) {
            throw new ConflictException("A product with this barcode already exists in the catalog.");
        }
    }

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
    public Product createProductCustom(Product product,MultipartFile image) {
        checkUniqueBarcodeCustom(product.getBarcode(), product.getCreatedBy().getId());
        return updateProductImage(product, image, null);
    }

    @Transactional
    public Product createProductOpenFoodFacts(String barcode,UUID userId) {

        checkUniqueBarcode(barcode);

        Optional<Product> openFoodFactsProduct = findProductInOpenFoodFacts(barcode);
        
        if (openFoodFactsProduct.isEmpty()) {
            throw new ResourceNotFoundException("Product not found in OpenFoodFacts");
        }
        
        Product product = openFoodFactsProduct.get();
        product.setIsCustom(false);
        product = updateProductImage(product, null, product.getImageUrl());
        return product;
    }
}
