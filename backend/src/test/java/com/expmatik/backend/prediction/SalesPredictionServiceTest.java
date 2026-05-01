package com.expmatik.backend.prediction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.expmatik.backend.exceptions.PredictionNotAvailableException;
import com.expmatik.backend.prediction.DTOs.PredictionResponse;
import com.expmatik.backend.sale.SaleRepository;
import com.expmatik.backend.sale.TransactionStatus;
import com.expmatik.backend.user.User;

@ExtendWith(MockitoExtension.class)
public class SalesPredictionServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @InjectMocks
    private SalesPredictionService salesPredictionService;

    private User user;
    private String barcode;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        barcode = "1234567890123";
    }

    private List<Object[]> buildSamples(int numMonths, long salesPerMonth) {
        List<Object[]> samples = new ArrayList<>();
        for (int i = 0; i < numMonths; i++) {
            int month = (i * 3 % 12) + 1; // spreads across months: 1, 4, 7, 10, ...
            samples.add(new Object[]{2024, barcode, month, salesPerMonth});
        }
        return samples;
    }

    // == predict tests ==

    @Nested
    @DisplayName("predict")
    class PredictTests {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("predict - valid data should return PredictionResponse with 12 monthly predictions")
            void testPredict_ValidData_ReturnsPredictionResponseWith12Months() {
                List<Object[]> samples = buildSamples(4, 6L);

                when(saleRepository.findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode, user.getId()))
                    .thenReturn(samples);

                PredictionResponse response = salesPredictionService.predict(barcode, user);

                assertNotNull(response);
                assertEquals(barcode, response.barcode());
                assertEquals(12, response.predictions().size());
                verify(saleRepository).findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode, user.getId());
            }

            @Test
            @DisplayName("predict - all 12 monthly predictions should have non-negative values")
            void testPredict_ValidData_AllPredictionsAreNonNegative() {
                List<Object[]> samples = buildSamples(4, 6L);

                when(saleRepository.findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode, user.getId()))
                    .thenReturn(samples);

                PredictionResponse response = salesPredictionService.predict(barcode, user);

                response.predictions().forEach(p ->
                    assertTrue(p.predictedSales() >= 0.0,
                        "El mes " + p.month() + " tiene una predicción negativa: " + p.predictedSales())
                );
            }

            @Test
            @DisplayName("predict - model should be cached on second call with same barcode")
            void testPredict_SameBarcode_ModelIsCachedAndRepositoryCalledOnce() {
                List<Object[]> samples = buildSamples(4, 6L);

                when(saleRepository.findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode, user.getId()))
                    .thenReturn(samples);

                salesPredictionService.predict(barcode, user);
                salesPredictionService.predict(barcode, user);

                verify(saleRepository, times(1))
                    .findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode, user.getId());
            }

            @Test
            @DisplayName("predict - different barcodes should build independent models")
            void testPredict_DifferentBarcodes_BuildSeparateModels() {
                String barcode2 = "9876543210987";
                List<Object[]> samples1 = buildSamples(4, 6L);
                List<Object[]> samples2 = buildSamples(4, 8L);

                when(saleRepository.findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode, user.getId()))
                    .thenReturn(samples1);
                when(saleRepository.findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode2, user.getId()))
                    .thenReturn(samples2);

                salesPredictionService.predict(barcode, user);
                salesPredictionService.predict(barcode2, user);

                assertEquals(2, salesPredictionService.getModelsByProduct().size());
                verify(saleRepository).findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode, user.getId());
                verify(saleRepository).findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode2, user.getId());
            }

            @Test
            @DisplayName("predict - response should contain the barcode used in the request")
            void testPredict_ValidData_ResponseContainsCorrectBarcode() {
                List<Object[]> samples = buildSamples(4, 6L);

                when(saleRepository.findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode, user.getId()))
                    .thenReturn(samples);

                PredictionResponse response = salesPredictionService.predict(barcode, user);

                assertEquals(barcode, response.barcode());
            }

            @Test
            @DisplayName("predict - predictions should cover all 12 months in order")
            void testPredict_ValidData_PredictionsCoverAllMonthsInOrder() {
                List<Object[]> samples = buildSamples(4, 6L);

                when(saleRepository.findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode, user.getId()))
                    .thenReturn(samples);

                PredictionResponse response = salesPredictionService.predict(barcode, user);

                for (int m = 1; m <= 12; m++) {
                    int month = m;
                    assertTrue(response.predictions().stream()
                        .anyMatch(p -> p.month().getValue() == month),
                        "Falta la predicción para el mes " + month);
                }
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("predict - fewer than 4 months of data should throw PredictionNotAvailableException")
            void testPredict_FewerThan4Samples_ThrowsPredictionNotAvailableException() {
                List<Object[]> samples = buildSamples(3, 10L);

                when(saleRepository.findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode, user.getId()))
                    .thenReturn(samples);

                assertThrows(PredictionNotAvailableException.class, () ->
                    salesPredictionService.predict(barcode, user)
                );
                verify(saleRepository).findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode, user.getId());
            }

            @Test
            @DisplayName("predict - no data should throw PredictionNotAvailableException")
            void testPredict_NoSamples_ThrowsPredictionNotAvailableException() {
                when(saleRepository.findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode, user.getId()))
                    .thenReturn(List.of());

                assertThrows(PredictionNotAvailableException.class, () ->
                    salesPredictionService.predict(barcode, user)
                );
            }

            @Test
            @DisplayName("predict - 4 months but total sales below 20 should throw PredictionNotAvailableException")
            void testPredict_SufficientMonthsButInsufficientTotalSales_ThrowsPredictionNotAvailableException() {
                // 4 months × 4 sales = 16 < 20 (MIN_TOTAL_SALES)
                List<Object[]> samples = buildSamples(4, 4L);

                when(saleRepository.findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode, user.getId()))
                    .thenReturn(samples);

                assertThrows(PredictionNotAvailableException.class, () ->
                    salesPredictionService.predict(barcode, user)
                );
            }

            @Test
            @DisplayName("predict - exactly 19 total sales should throw PredictionNotAvailableException")
            void testPredict_ExactlyBelowMinTotalSales_ThrowsPredictionNotAvailableException() {
                // 4 months with uneven counts summing to 19
                List<Object[]> samples = List.of(
                    new Object[]{2024, barcode, 1, 5L},
                    new Object[]{2024, barcode, 4, 5L},
                    new Object[]{2024, barcode, 7, 5L},
                    new Object[]{2024, barcode, 10, 4L}
                );

                when(saleRepository.findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode, user.getId()))
                    .thenReturn(samples);

                assertThrows(PredictionNotAvailableException.class, () ->
                    salesPredictionService.predict(barcode, user)
                );
            }
        }
    }

    // == getModelsByProduct tests ==

    @Nested
    @DisplayName("getModelsByProduct")
    class GetModelsByProductTests {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("getModelsByProduct - should return empty map when no predictions have been made")
            void testGetModelsByProduct_NoPredicitions_ReturnsEmptyMap() {
                assertTrue(salesPredictionService.getModelsByProduct().isEmpty());
            }

            @Test
            @DisplayName("getModelsByProduct - should contain model after a successful predict call")
            void testGetModelsByProduct_AfterPredict_ContainsModel() {
                List<Object[]> samples = buildSamples(4, 6L);

                when(saleRepository.findMonthlySalesByBarcode(TransactionStatus.SUCCESS, barcode, user.getId()))
                    .thenReturn(samples);

                salesPredictionService.predict(barcode, user);

                assertTrue(salesPredictionService.getModelsByProduct().containsKey(barcode));
            }
        }
    }
}
