package com.expmatik.backend.product;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.expmatik.backend.product.DTOs.ProductCreate;
import com.expmatik.backend.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Endpoints para gestionar productos e imágenes")
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
        if(product.getIsCustom() && product.getCreatedBy().equals(userService.getUserProfile())) {
            return ResponseEntity.badRequest().body("You are not authorized to view this product.");
        }
        return ResponseEntity.ok(productService.findById(id));
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<?> getProductByBarcode(@PathVariable String barcode) {
        Product product = productService.findByBarcode(barcode);
        if(product.getIsCustom() && product.getCreatedBy().equals(userService.getUserProfile())) {
            return ResponseEntity.badRequest().body("You are not authorized to view this product.");
        }
        return ResponseEntity.ok(productService.findByBarcode(barcode));
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

    @GetMapping("/custom/user/{userId}")
    @Operation(summary = "Obtener productos personalizados por usuario", description = "Devuelve una lista de productos personalizados creados por un usuario específico")
    public ResponseEntity<?> getCustomProductsByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(productService.getCustomProductsByUserId(userId));
    }

    @GetMapping("/non-custom")
    @Operation(summary = "Obtener productos no personalizados", description = "Devuelve una lista de productos no personalizados")
    public ResponseEntity<?> getAllNonCustomProducts() {
        return ResponseEntity.ok(productService.getAllNonCustomProducts());
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Crear nuevo producto", description = "Crea un nuevo producto personalizado o no personalizado. Para productos personalizados se requiere un archivo de imagen, para no personalizados se requiere una URL de imagen.")
    public ResponseEntity<?> createProduct(
            @RequestParam("name") String name,
            @RequestParam("brand") String brand,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "isPerishable", required = false, defaultValue = "false") Boolean isPerishable,
            @RequestParam("barcode") String barcode,
            @RequestParam(value = "isCustom", required = false, defaultValue = "false") Boolean isCustom,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "imageUrl", required = false) String imageUrl
    ) {
        ProductCreate productDTO = new ProductCreate(name, brand, description, isPerishable, barcode, isCustom);
        Product createdProduct = productService.createProduct(productDTO, file, imageUrl);
        return ResponseEntity.ok(createdProduct);
    }
}
