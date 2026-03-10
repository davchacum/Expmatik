package com.expmatik.backend.batch;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expmatik.backend.batch.DTOs.BatchCreate;
import com.expmatik.backend.batch.DTOs.BatchResponse;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/batches")
@Tag(name = "Batches", description = "Endpoints para gestionar lotes de productos en facturas")
public class BatchController {

    private final BatchService batchService;
    private final UserService userService;

    public BatchController(BatchService batchService, UserService userService) {
        this.batchService = batchService;
        this.userService = userService;
    }
    @PostMapping()
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> createBatch(@Valid @RequestBody BatchCreate batchCreate,@RequestParam UUID invoiceId) {

        User user = userService.getUserProfile();

        Batch batch = batchService.createBatch(user.getId(), batchCreate, invoiceId);
        return ResponseEntity.ok().body(BatchResponse.fromBatch(batch));
    }

    @PutMapping("/{batchId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> updateBatch(@PathVariable UUID batchId, @Valid @RequestBody BatchCreate batchCreate) {

        User user = userService.getUserProfile();

        Batch batch = batchService.updateBatch(user.getId(), batchId, batchCreate);
        return ResponseEntity.ok().body(BatchResponse.fromBatch(batch));
    }

    @DeleteMapping("/{batchId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> deleteBatch(@PathVariable UUID batchId) {
        User user = userService.getUserProfile();
        batchService.deleteBatch(user.getId(), batchId);
        return ResponseEntity.noContent().build();
    }
}
