package com.expmatik.backend.maintenance;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expmatik.backend.maintenance.DTOs.MaintenanceCreate;
import com.expmatik.backend.maintenance.DTOs.MaintenanceResponse;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/maintenances")
@Tag(name = "MaintenanceController", description = "Controlador para gestionar los mantenimientos de los equipos.")
@Validated
public class MaintenanceController {

    public final MaintenanceService maintenanceService;
    public final UserService userService;

    public MaintenanceController(MaintenanceService maintenanceService, UserService userService) {
        this.maintenanceService = maintenanceService;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMaintenanceById(@PathVariable UUID id) {
        User user = userService.getUserProfile();
        return ResponseEntity.ok(MaintenanceResponse.fromMaintenance(maintenanceService.findById(id, user)));
    }

   @GetMapping
    public ResponseEntity<?> searchMaintenances(
        @RequestParam(required = false) MaintenanceStatus status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @ParameterObject Pageable pageable
    ) {
        User user = userService.getUserProfile();
        return ResponseEntity.ok(MaintenanceResponse.fromMaintenancePage(maintenanceService.searchMaintenances(user, status, startDate, endDate, pageable)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateMaintenanceStatus(@PathVariable UUID id, @RequestParam MaintenanceStatus newStatus) {
        User user = userService.getUserProfile();
        return ResponseEntity.ok(MaintenanceResponse.fromMaintenance(maintenanceService.updateStatus(id, newStatus, user)));
    }

    @PostMapping
    public ResponseEntity<?> createMaintenance(@RequestBody MaintenanceCreate maintenanceCreate) {
        User administrator = userService.getUserProfile();
        return ResponseEntity.ok(MaintenanceResponse.fromMaintenance(maintenanceService.createMaintenance(maintenanceCreate, administrator)));
    }
}