package com.expmatik.backend.productInfo;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expmatik.backend.productInfo.DTOs.ProductInfoResponse;
import com.expmatik.backend.productInfo.DTOs.ProductInfoUpdate;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/product-info")
@Tag(name = "ProductInfo", description = "Endpoints para gestionar la información de productos específicos de cada usuario")
@Validated
public class ProductInfoController {

    private final ProductInfoService productInfoService;
    private final UserService userService;

    public ProductInfoController(ProductInfoService productInfoService, UserService userService) {
        this.productInfoService = productInfoService;
        this.userService = userService;
    }

    @GetMapping()
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> getAllProductInfoForUser(@ParameterObject Pageable pageable) {
        User user = userService.getUserProfile();
        return ResponseEntity.ok(ProductInfoResponse.fromProductInfoPage(productInfoService.findAllByUserIdOrderByStockQuantityDesc(user.getId(), pageable)));
    }

    @PutMapping("/{productInfoId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> updateProductInfo(@PathVariable UUID productInfoId, @RequestBody ProductInfoUpdate updatedInfo) {
        User user = userService.getUserProfile();
        ProductInfo updatedProductInfo = productInfoService.updateProductInfo(productInfoId, user, updatedInfo);
        return ResponseEntity.ok(ProductInfoResponse.fromProductInfo(updatedProductInfo));
    }

    @GetMapping("/get-or-create-product/{productId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> getOrCreateProductInfo(@PathVariable UUID productId) {
        User user = userService.getUserProfile();
        ProductInfo productInfo = productInfoService.getOrCreateProductInfo(productId, user, null);
        return ResponseEntity.ok(ProductInfoResponse.fromProductInfo(productInfo));
    }

    @PatchMapping("/{productInfoId}/edit-stock")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> editStockQuantity(@PathVariable UUID productInfoId, @RequestBody Integer newStockQuantity) {
        User user = userService.getUserProfile();
        ProductInfo updatedProductInfo = productInfoService.editStockQuantity(productInfoId, user, newStockQuantity, null);
        return ResponseEntity.ok(ProductInfoResponse.fromProductInfo(updatedProductInfo));
    }

}
