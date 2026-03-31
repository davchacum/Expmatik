package com.expmatik.backend.vendingMachine;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expmatik.backend.user.Role;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;
import com.expmatik.backend.vendingMachine.DTOs.VendingMachineCreate;
import com.expmatik.backend.vendingMachine.DTOs.VendingMachineResponse;
import com.expmatik.backend.vendingMachine.DTOs.VendingMachineUpdate;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/vending-machines")
@Tag(name = "VendingMachineController", description = "Controlador para gestionar las máquinas expendedoras.")
@Validated
public class VendingMachineController {

    public final VendingMachineService vendingMachineService;
    public final UserService userService;

    public VendingMachineController(VendingMachineService vendingMachineService, UserService userService) {
        this.vendingMachineService = vendingMachineService;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> getVendingMachineById(@PathVariable UUID id) {
        User currentUser = userService.getUserProfile();
        VendingMachine response = vendingMachineService.getVendingMachineById(id, currentUser);
        return ResponseEntity.ok(VendingMachineResponse.fromVendingMachine(response));
    }

    @GetMapping()
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> getAllVendingMachines(Pageable pageable) {
        User currentUser = userService.getUserProfile();
        Page<VendingMachine> vendingMachinePage = vendingMachineService.listVendingMachines(currentUser, pageable);
        return ResponseEntity.ok(VendingMachineResponse.fromVendingMachinePage(vendingMachinePage));
    }

    @PostMapping()
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> createVendingMachine(@RequestBody @Validated VendingMachineCreate vendingMachineCreate) {
        User currentUser = userService.getUserProfile();
        VendingMachine response = vendingMachineService.createVendingMachine(vendingMachineCreate, currentUser);
        return ResponseEntity.ok(VendingMachineResponse.fromVendingMachine(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> updateVendingMachine(@PathVariable UUID id, @RequestBody @Validated VendingMachineUpdate vendingMachineUpdate) {
        User currentUser = userService.getUserProfile();
        VendingMachine vendingMachine = vendingMachineService.updateVendingMachine(id, vendingMachineUpdate, currentUser);
        return ResponseEntity.ok(VendingMachineResponse.fromVendingMachine(vendingMachine));
    }
}
