package com.expmatik.backend.prediction.DTOs;

import java.util.List;

public record PredictionResponse(
        String barcode,
        double modelR2,
        List<MonthlyPrediction> predictions
) {}
