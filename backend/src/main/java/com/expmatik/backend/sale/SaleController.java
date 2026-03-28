package com.expmatik.backend.sale;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expmatik.backend.sale.DTOs.SaleCreate;
import com.expmatik.backend.sale.DTOs.SaleResponse;
import com.expmatik.backend.user.Role;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sales")
@Tag(name = "SaleController", description = "Controlador para gestionar las ventas realizadas en las máquinas expendedoras.")
@Validated
public class SaleController {

    private final SaleService saleService;
    private final UserService userService;

    public SaleController(SaleService saleService, UserService userService) {
        this.saleService = saleService;
        this.userService = userService;
    }

    private void checkUserAuthorization(User currentUser) throws AccessDeniedException {
        if(!currentUser.getRole().equals(Role.ADMINISTRATOR)) {
            throw new AccessDeniedException("You are not authorized to modify this machine.");
        }
    }

    @GetMapping("{id}")
    public ResponseEntity<?> getSaleById(UUID id) throws AccessDeniedException {
        User currentUser = userService.getUserProfile();
        checkUserAuthorization(currentUser);
        return ResponseEntity.ok(SaleResponse.fromSale(saleService.getSaleById(id, currentUser)));
    }

    @PostMapping
    public ResponseEntity<?> createSale(@RequestBody @Valid SaleCreate saleCreate) throws AccessDeniedException {
        User currentUser = userService.getUserProfile();
        checkUserAuthorization(currentUser);
        return ResponseEntity.ok(SaleResponse.fromSale(saleService.createSale(saleCreate, currentUser)));
    }



}
