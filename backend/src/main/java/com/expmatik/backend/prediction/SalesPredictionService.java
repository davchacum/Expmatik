package com.expmatik.backend.prediction;

import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.expmatik.backend.exceptions.PredictionNotAvailableException;
import com.expmatik.backend.prediction.DTOs.MonthlyPrediction;
import com.expmatik.backend.prediction.DTOs.PredictionResponse;
import com.expmatik.backend.sale.SaleRepository;
import com.expmatik.backend.sale.TransactionStatus;
import com.expmatik.backend.user.User;

@Service
public class SalesPredictionService {

    private static final int MIN_SAMPLES     = 4;
    private static final int MIN_TOTAL_SALES = 20;

    private final SaleRepository saleRepository;

    private final Map<String, SalesPredictionModel> modelsByProduct = new HashMap<>();

    public SalesPredictionService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public PredictionResponse predict(String barcode,User currentUser) {
        SalesPredictionModel model = getModel(barcode,currentUser);



        List<MonthlyPrediction> predictions = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            predictions.add(new MonthlyPrediction(Month.of(m), model.predict(m)));
        }
        return new PredictionResponse(barcode, model.getR2(), predictions);
    }

    public Map<String, SalesPredictionModel> getModelsByProduct() {
        return modelsByProduct;
    }

    private SalesPredictionModel getModel(String barcode,User currentUser) {
        return modelsByProduct.computeIfAbsent(barcode, b -> buildModel(b,currentUser));
    }

    private SalesPredictionModel buildModel(String barcode,User currentUser) {
        List<Object[]> samples = saleRepository.findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode,currentUser.getId());

        if (samples.size() < MIN_SAMPLES) {
            throw new PredictionNotAvailableException(
                    "El producto '" + barcode + "' no tiene datos suficientes para generar una predicción. " +
                    "Se requieren al menos " + MIN_SAMPLES + " meses distintos con un total de " +
                    MIN_TOTAL_SALES + " unidades vendidas.");
        }

        List<double[]> features   = new ArrayList<>();
        List<Double>   targets    = new ArrayList<>();
        long           totalCount = 0;

        for (Object[] row : samples) {
            int  month = ((Number) row[2]).intValue();
            long count = ((Number) row[3]).longValue();

            totalCount += count;
            features.add(SalesPredictionModel.buildFeatureVector(month));
            targets.add((double) count);
        }

        if (totalCount < MIN_TOTAL_SALES) {
            throw new PredictionNotAvailableException(
                    "El producto '" + barcode + "' no tiene datos suficientes para generar una predicción. " +
                    "Se requieren al menos " + MIN_SAMPLES + " meses distintos con un total de " +
                    MIN_TOTAL_SALES + " unidades vendidas.");
        }

        SalesPredictionModel model = new SalesPredictionModel();
        model.train(features, targets);
        return model;
    }
}
