package com.expmatik.backend.prediction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.prediction-seeder.enabled=true")
@AutoConfigureMockMvc
public class SalesPredictionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String ENDPOINT = "/api/analytics/predict";
    private static final String VALID_BARCODE = "20000144";

    // == POST /api/analytics/predict ==

    @Nested
    @DisplayName("POST /api/analytics/predict")
    class PostPredict {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("POST /predict - barcode with sufficient data should return 200 with 12 monthly predictions")
            @WithUserDetails("prediction@expmatik.com")
            public void testPredict_ValidBarcodeWithData_Returns200With12Predictions() throws Exception {
                mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\": \"" + VALID_BARCODE + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.barcode").value(VALID_BARCODE))
                    .andExpect(jsonPath("$.predictions").isArray())
                    .andExpect(jsonPath("$.predictions.length()").value(12));
            }

            @Test
            @DisplayName("POST /predict - response should contain modelR2 field")
            @WithUserDetails("prediction@expmatik.com")
            public void testPredict_ValidBarcode_ResponseContainsR2() throws Exception {
                mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\": \"" + VALID_BARCODE + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.modelR2").exists());
            }

            @Test
            @DisplayName("POST /predict - each monthly prediction should have a month and a non-negative predictedSales")
            @WithUserDetails("prediction@expmatik.com")
            public void testPredict_ValidBarcode_EachMonthHasNonNegativePrediction() throws Exception {
                mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\": \"" + VALID_BARCODE + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.predictions[0].month").exists())
                    .andExpect(jsonPath("$.predictions[0].predictedSales").isNumber())
                    .andExpect(jsonPath("$.predictions[11].month").exists())
                    .andExpect(jsonPath("$.predictions[11].predictedSales").isNumber());
            }

            @Test
            @DisplayName("POST /predict - second call with same barcode should return same response (model cached)")
            @WithUserDetails("prediction@expmatik.com")
            public void testPredict_SecondCallSameBarcode_ReturnsSameResult() throws Exception {
                String body = "{\"barcode\": \"" + VALID_BARCODE + "\"}";

                String r2First = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

                String r2Second = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

                org.junit.jupiter.api.Assertions.assertEquals(r2First, r2Second);
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("POST /predict - barcode with no sales should return 422 Unprocessable Entity")
            @WithUserDetails("prediction@expmatik.com")
            public void testPredict_BarcodeWithNoSalesData_Returns422() throws Exception {
                mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\": \"99999999\"}"))
                    .andExpect(status().isUnprocessableEntity());
            }

            @Test
            @DisplayName("POST /predict - admin user with no prediction sales should return 422")
            @WithUserDetails("admin@expmatik.com")
            public void testPredict_AdminUserWithNoSalesForBarcode_Returns422() throws Exception {
                mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\": \"" + VALID_BARCODE + "\"}"))
                    .andExpect(status().isUnprocessableEntity());
            }

            @Test
            @DisplayName("POST /predict - MAINTAINER user should return 403 Forbidden")
            @WithUserDetails("repo@expmatik.com")
            public void testPredict_MaintainerUser_Returns403() throws Exception {
                mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\": \"" + VALID_BARCODE + "\"}"))
                    .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("POST /predict - unauthenticated request should return 401 Unauthorized")
            public void testPredict_Unauthenticated_Returns401() throws Exception {
                mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\": \"" + VALID_BARCODE + "\"}"))
                    .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("POST /predict - missing barcode field should return 400 Bad Request")
            @WithUserDetails("prediction@expmatik.com")
            public void testPredict_MissingBarcode_Returns400() throws Exception {
                mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                    .andExpect(status().isBadRequest());
            }
        }
    }
}
