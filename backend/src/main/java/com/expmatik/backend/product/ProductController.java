package com.expmatik.backend.product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.expmatik.backend.product.DTOs.ProductResponse;
import com.expmatik.backend.product.DTOs.ProductCreate;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;
import com.expmatik.backend.validation.ValidBarcode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Endpoints para gestionar productos e imágenes")
@Validated
public class ProductController {

    private final ProductService productService;
    private final UserService userService;

    public ProductController(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable UUID id) {
        Product product = productService.findById(id);
        if(product.getIsCustom() && !product.getCreatedBy().getId().equals(userService.getUserProfile().getId())) {
            return ResponseEntity.badRequest().body("You are not authorized to view this product.");
        }
        ProductResponse productResponseDTO = ProductResponse.fromProduct(productService.findById(id));
        return ResponseEntity.ok(productResponseDTO);
    }

    @PutMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Actualizar imagen del producto", description = "Sube un archivo (productos personalizados) o establece una URL (productos no personalizados)")
    public ResponseEntity<?> updateProductImage(
            @PathVariable UUID id,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "imageUrl", required = false) String imageUrl
    ) {
        Product product = productService.findById(id);
        Product updatedProduct = productService.updateProductImage(product, file, imageUrl);
        ProductResponse productResponseDTO = ProductResponse.fromProduct(updatedProduct);
        return ResponseEntity.ok(productResponseDTO);
    }

    @GetMapping("/custom")
    @Operation(summary = "Obtener productos personalizados por usuario", description = "Devuelve una lista de productos personalizados creados por un usuario específico")
    public ResponseEntity<?> getCustomProductsByUserId() {
        User currentUser = userService.getUserProfile();
        List<Product> products = productService.getCustomProductsByUserId(currentUser.getId());
        List<ProductResponse> productResponseDTOs = ProductResponse.fromProductList(products);
        return ResponseEntity.ok(productResponseDTOs);
    }

    @GetMapping
    @Operation(summary = "Buscar productos", description = "Devuelve una lista de productos no personalizados (catálogo) + productos personalizados del usuario autenticado. Soporta filtros opcionales por nombre, marca y código de barras")
    public ResponseEntity<?> searchAllProducts(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "barcode", required = false) @ValidBarcode String barcode
    ) {
        User currentUser = userService.getUserProfile();
        List<Product> products = productService.searchAllProducts(currentUser.getId(), name, brand, barcode);
        List<ProductResponse> productResponseDTOs = ProductResponse.fromProductList(products);
        return ResponseEntity.ok(productResponseDTOs);
    }

    @PostMapping(value = "/custom", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Crear nuevo producto", description = "Crea un nuevo producto personalizado o no personalizado. Para productos personalizados se requiere un archivo de imagen, para no personalizados se requiere una URL de imagen.")
    public ResponseEntity<?> createCustomProduct(
            @RequestParam ProductCreate productCreate,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        
        User currentUser = userService.getUserProfile();
        Product product = productCreate.toEntity();
        product.setCreatedBy(currentUser);
        product.setIsCustom(true);
        product.setImageUrl(null);
        Product createdProduct = productService.createProductCustom( product, file);
        ProductResponse productResponseDTO = ProductResponse.fromProduct(createdProduct);
        return ResponseEntity.ok(productResponseDTO);
    }

    @PostMapping(value = "/non-custom")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Crear nuevo producto no personalizado", description = "Crea un nuevo producto no personalizado. Se requiere una URL de imagen.")
    public ResponseEntity<?> createNonCustomProduct(
            @RequestParam("barcode") @ValidBarcode String barcode
    ) {
        User currentUser = userService.getUserProfile();
        Product product = productService.createProductOpenFoodFacts(barcode, currentUser.getId());
        ProductResponse productResponseDTO = ProductResponse.fromProduct(product);
        return ResponseEntity.ok(productResponseDTO);
    }

    @GetMapping("/openfoodfacts/{barcode}")
    @Operation(summary = "Obtener producto de OpenFoodFacts por código de barras", description = "Busca un producto en la API de OpenFoodFacts utilizando su código de barras. Devuelve los datos del producto si se encuentra, o un error si no se encuentra o si el código de barras no es válido.")
    public ResponseEntity<?> findProductInOpenFoodFacts(@PathVariable @ValidBarcode String barcode) {
        Optional<Product> product = productService.findProductInOpenFoodFacts(barcode);
        if (!product.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        ProductResponse productResponseDTO = ProductResponse.fromProduct(product.get());
        return ResponseEntity.ok(productResponseDTO);
    }
}
