package com.expmatik.backend.prediction;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expmatik.backend.prediction.DTOs.PredictionRequest;
import com.expmatik.backend.prediction.DTOs.PredictionResponse;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/analytics/predict")
@Tag(name = "Predicción de Ventas", description = "Predicción de ventas mensuales mediante regresión lineal con estacionalidad")
public class SalesPredictionController {

    private final SalesPredictionService predictionService;
    private final UserService userService;

    public SalesPredictionController(SalesPredictionService predictionService, UserService userService) {
        this.predictionService = predictionService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<PredictionResponse> predict(@Valid @RequestBody PredictionRequest request) {
        User currentUser = userService.getUserProfile();
        return ResponseEntity.ok(predictionService.predict(request.barcode(), currentUser));
    }
}
