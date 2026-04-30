package com.expmatik.backend.prediction.DTOs;

import java.time.Month;

public record MonthlyPrediction(
        Month month,
        double predictedSales
) {}
