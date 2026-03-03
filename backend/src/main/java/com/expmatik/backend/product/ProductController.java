package com.expmatik.backend.product;

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

import com.expmatik.backend.product.DTOs.ProductCreateCustom;
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
        return ResponseEntity.ok(productService.findById(id));
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<?> getProductByBarcode(@PathVariable @ValidBarcode String barcode) {
        User currentUser = userService.getUserProfile();
        return ResponseEntity.ok(productService.findByBarcode(currentUser.getId(), barcode));
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
        return ResponseEntity.ok(updatedProduct);
    }

    @GetMapping("/custom")
    @Operation(summary = "Obtener productos personalizados por usuario", description = "Devuelve una lista de productos personalizados creados por un usuario específico")
    public ResponseEntity<?> getCustomProductsByUserId() {
        User currentUser = userService.getUserProfile();
        return ResponseEntity.ok(productService.getCustomProductsByUserId(currentUser.getId()));
    }

    @GetMapping
    @Operation(summary = "Buscar productos", description = "Devuelve una lista de productos no personalizados (catálogo) + productos personalizados del usuario autenticado. Soporta filtros opcionales por nombre, marca y código de barras")
    public ResponseEntity<?> searchAllProducts(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "barcode", required = false) @ValidBarcode String barcode
    ) {
        User currentUser = userService.getUserProfile();
        return ResponseEntity.ok(productService.searchAllProducts(currentUser.getId(), name, brand, barcode));
    }

    @PostMapping(value = "/custom", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Crear nuevo producto", description = "Crea un nuevo producto personalizado o no personalizado. Para productos personalizados se requiere un archivo de imagen, para no personalizados se requiere una URL de imagen.")
    public ResponseEntity<?> createCustomProduct(
            @RequestParam("name") String name,
            @RequestParam("brand") String brand,
            @RequestParam(value = "description", required = false, defaultValue = "") String description,
            @RequestParam(value = "isPerishable", required = false, defaultValue = "false") Boolean isPerishable,
            @RequestParam("barcode") @ValidBarcode String barcode,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        
        User currentUser = userService.getUserProfile();
        ProductCreateCustom productDTO = new ProductCreateCustom(name, brand, description, isPerishable, barcode, file,currentUser);
        Product createdProduct = productService.createProductCustom(currentUser.getId(), productDTO, file);
        return ResponseEntity.ok(createdProduct);
    }

    @PostMapping(value = "/non-custom")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Crear nuevo producto no personalizado", description = "Crea un nuevo producto no personalizado. Se requiere una URL de imagen.")
    public ResponseEntity<?> createNonCustomProduct(
            @RequestParam("barcode") @ValidBarcode String barcode
    ) {
        User currentUser = userService.getUserProfile();
        Product product = productService.createProductOpenFoodFacts(barcode, currentUser.getId());
        return ResponseEntity.ok(product);
    }
}
