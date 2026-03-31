package com.expmatik.backend.vendingSlot;

import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expmatik.backend.user.Role;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;
import com.expmatik.backend.validation.ValidBarcode;
import com.expmatik.backend.vendingSlot.DTOs.VendingSlotResponse;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/vending-slots")
@Tag(name = "VendingSlotController", description = "Controlador para gestionar los slots de las máquinas expendedoras.")
public class VendingSlotController {

    public final VendingSlotService vendingSlotService;
    public final UserService userService;
    public final ExpirationBatchService expirationBatchService;

    public VendingSlotController(VendingSlotService vendingSlotService, UserService userService, ExpirationBatchService expirationBatchService) {
        this.vendingSlotService = vendingSlotService;
        this.userService = userService;
        this.expirationBatchService = expirationBatchService;
    }

    @GetMapping("{machineId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> getVendingSlotsByMachineId(@PathVariable UUID machineId) throws AccessDeniedException {
        User currentUser = userService.getUserProfile();
        List<VendingSlot> vendingSlots = vendingSlotService.getVendingSlotsByUserIdAndMachineId(machineId, currentUser);
        return ResponseEntity.ok(VendingSlotResponse.fromVendingSlotList(vendingSlots));
    }

    @PatchMapping("{vendingSlotId}/assign-product/{barcode}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> assignProductToVendingSlot(@PathVariable UUID vendingSlotId, @PathVariable @ValidBarcode String barcode) throws AccessDeniedException {
        User currentUser = userService.getUserProfile();
        VendingSlot vendingSlot = vendingSlotService.assignProductToVendingSlot(vendingSlotId, barcode, currentUser);
        return ResponseEntity.ok(VendingSlotResponse.fromVendingSlot(vendingSlot));
    }

    @PatchMapping("{vendingSlotId}/unassign-product")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> unassignProductFromVendingSlot(@PathVariable UUID vendingSlotId) throws AccessDeniedException {
        User currentUser = userService.getUserProfile();
        VendingSlot vendingSlot = vendingSlotService.assignProductToVendingSlot(vendingSlotId, null, currentUser);
        return ResponseEntity.ok(VendingSlotResponse.fromVendingSlot(vendingSlot));
    }

    @PatchMapping("{vendingSlotId}/block")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> blockVendingSlot(@PathVariable UUID vendingSlotId) throws AccessDeniedException {
        User currentUser = userService.getUserProfile();
        VendingSlot vendingSlot = vendingSlotService.updateBlockStatus(vendingSlotId, true, currentUser);
        return ResponseEntity.ok(VendingSlotResponse.fromVendingSlot(vendingSlot));
    }

    @PatchMapping("{vendingSlotId}/unblock")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> unblockVendingSlot(@PathVariable UUID vendingSlotId) throws AccessDeniedException {
        User currentUser = userService.getUserProfile();
        VendingSlot vendingSlot = vendingSlotService.updateBlockStatus(vendingSlotId, false, currentUser);
        return ResponseEntity.ok(VendingSlotResponse.fromVendingSlot(vendingSlot));
    }

    @PatchMapping("{vendingSlotId}/increment-stock")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> addStockToVendingSlot(@PathVariable UUID vendingSlotId,  @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate expirationDate, @RequestParam(required = true) Integer quantity) throws AccessDeniedException {
        User currentUser = userService.getUserProfile();
        VendingSlot vendingSlot = vendingSlotService.addStockToVendingSlot(vendingSlotId, quantity, expirationDate, currentUser);
        return ResponseEntity.ok(VendingSlotResponse.fromVendingSlot(vendingSlot));
    }

    @PatchMapping("{vendingSlotId}/decrement-stock")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> removeStockFromVendingSlot(@PathVariable UUID vendingSlotId) throws AccessDeniedException {
        User currentUser = userService.getUserProfile();
        VendingSlot vendingSlot = vendingSlotService.popStockFromVendingSlot(vendingSlotId, currentUser);
        return ResponseEntity.ok(VendingSlotResponse.fromVendingSlot(vendingSlot));
    }

    @GetMapping("{vendingSlotId}/expiration-batches")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> getExpirationBatchesByVendingSlotId(@PathVariable UUID vendingSlotId) throws AccessDeniedException {
        User currentUser = userService.getUserProfile();
        List<ExpirationBatch> expirationBatches = expirationBatchService.getExpirationBatchesByVendingSlotId(vendingSlotId, currentUser);
        return ResponseEntity.ok(expirationBatches);
    }
}
