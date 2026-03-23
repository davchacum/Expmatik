package com.expmatik.backend.vendingSlot;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expmatik.backend.user.Role;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;
import com.expmatik.backend.vendingSlot.DTOs.VendingSlotResponse;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/vending-slots")
@Tag(name = "VendingSlotController", description = "Controlador para gestionar los slots de las máquinas expendedoras.")
public class VendingSlotController {

    public final VendingSlotService vendingSlotService;
    public final UserService userService;

    public VendingSlotController(VendingSlotService vendingSlotService, UserService userService) {
        this.vendingSlotService = vendingSlotService;
        this.userService = userService;
    }

    @GetMapping("{machineId}")
    public ResponseEntity<?> getVendingSlotsByMachineId(@PathVariable UUID machineId) {
        User currentUser = userService.getUserProfile();
        if(currentUser.getRole().equals(Role.ADMINISTRATOR)) {
            return ResponseEntity.badRequest().body("You are not authorized to view this product.");
        }
        List<VendingSlot> vendingSlots = vendingSlotService.getVendingSlotsByUserIdAndMachineId(machineId, currentUser);
        return ResponseEntity.ok(VendingSlotResponse.fromVendingSlotList(vendingSlots));
    }

}
