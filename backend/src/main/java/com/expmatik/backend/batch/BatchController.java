package com.expmatik.backend.batch;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expmatik.backend.batch.DTOs.BatchCreate;
import com.expmatik.backend.batch.DTOs.BatchResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/batches")
@Tag(name = "Batches", description = "Endpoints para gestionar lotes de productos en facturas")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }
    @PostMapping()
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> createBatch(@Valid BatchCreate batchCreate,@RequestParam UUID invoiceId) {
        Batch batch = batchService.createBatch(batchCreate, invoiceId);
        return ResponseEntity.ok().body(BatchResponse.fromBatch(batch));
    }

    @PutMapping("/{batchId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> updateBatch(@PathVariable UUID batchId, @Valid BatchCreate batchCreate) {
        Batch batch = batchService.updateBatch(batchId, batchCreate);
        return ResponseEntity.ok().body(BatchResponse.fromBatch(batch));
    }

    @DeleteMapping("/{batchId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> deleteBatch(@PathVariable UUID batchId) {
        batchService.deleteBatch(batchId);
        return ResponseEntity.noContent().build();
    }
}
