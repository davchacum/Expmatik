package com.expmatik.backend.batch;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.batch.DTOs.BatchCreate;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class BatchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // == Pruebas para POST /api/batches == //

    @Nested
    @DisplayName("POST /api/batches")
    class CreateBatchTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testCreateBatch_ValidDataAndBelongsToUser_ShouldSucceed() throws Exception {

                BatchCreate batchCreate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "20000001"
                );

                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");


                String jsonRequest = objectMapper.writeValueAsString(batchCreate);

                mockMvc.perform(post("/api/batches")
                        .param("invoiceId", invoiceId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.expirationDate").value(batchCreate.expirationDate().toString()))
                        .andExpect(jsonPath("$.unitPrice").value(batchCreate.unitPrice()))
                        .andExpect(jsonPath("$.quantity").value(batchCreate.quantity()))
                        .andExpect(jsonPath("$.productId").value("00000000-0000-0000-0000-000000000001"))
                        .andExpect(jsonPath("$.productName").value("Leche Entera"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testCreateBatchAndProductInfo_ValidData_ShouldSucceed() throws Exception {

                BatchCreate batchCreate = new BatchCreate(
                    null,
                    new BigDecimal("5.99"),
                    10,
                    "20000002"
                );

                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                String jsonRequest = objectMapper.writeValueAsString(batchCreate);

                mockMvc.perform(post("/api/batches")
                        .param("invoiceId", invoiceId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.unitPrice").value(batchCreate.unitPrice()))
                        .andExpect(jsonPath("$.quantity").value(batchCreate.quantity()))
                        .andExpect(jsonPath("$.productId").value("00000000-0000-0000-0000-000000000002"))
                        .andExpect(jsonPath("$.productName").value("Pan de Molde"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testCreateBatch_WithOpenFoodFactsProduct_ShouldSucceed() throws Exception {
                Thread.sleep(2000);
                BatchCreate batchCreate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "4716982022201"
                );

                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                String jsonRequest = objectMapper.writeValueAsString(batchCreate);

                mockMvc.perform(post("/api/batches")
                        .param("invoiceId", invoiceId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.unitPrice").value(batchCreate.unitPrice()))
                        .andExpect(jsonPath("$.quantity").value(batchCreate.quantity()))
                        .andExpect(jsonPath("$.expirationDate").value(batchCreate.expirationDate().toString()))
                        .andExpect(jsonPath("$.productName").value("Choco Bom"));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testCreateBatch_NonExistingProduct_ShouldReturnBadRequest() throws Exception {

                BatchCreate batchCreate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "200000099"
                );

                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                String jsonRequest = objectMapper.writeValueAsString(batchCreate);

                mockMvc.perform(post("/api/batches")
                        .param("invoiceId", invoiceId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testCreateBatch_NonExistingInvoice_ShouldReturnBadRequest() throws Exception {

                BatchCreate batchCreate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "20000001"
                );

                UUID invoiceId = UUID.fromString("99000000-0000-0000-0000-000000000001");

                String jsonRequest = objectMapper.writeValueAsString(batchCreate);

                mockMvc.perform(post("/api/batches")
                        .param("invoiceId", invoiceId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isNotFound());
            }

            @Test
            @WithUserDetails("admin2@expmatik.com")
            void testCreateBatch_InvoiceNotOwned_ShouldReturnForbidden() throws Exception {

                BatchCreate batchCreate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "20000001"
                );

                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                String jsonRequest = objectMapper.writeValueAsString(batchCreate);

                mockMvc.perform(post("/api/batches")
                        .param("invoiceId", invoiceId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isForbidden());
            }

            @Test
            @WithUserDetails("repo@expmatik.com")
            void testCreateBatch_UnauthorizedUser_ShouldReturnForbidden() throws Exception {

                BatchCreate batchCreate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "20000001"
                );

                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                String jsonRequest = objectMapper.writeValueAsString(batchCreate);

                mockMvc.perform(post("/api/batches")
                        .param("invoiceId", invoiceId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isForbidden());
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testCreateBatch_WithPerishableProductAndMissingExpirationDate_ShouldReturnConflict() throws Exception {

                BatchCreate batchCreate = new BatchCreate(
                    null,
                    new BigDecimal("5.99"),
                    10,
                    "20000001"
                );

                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                String jsonRequest = objectMapper.writeValueAsString(batchCreate);

                mockMvc.perform(post("/api/batches")
                        .param("invoiceId", invoiceId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isConflict());
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testCreateBatch_WithNonPerishableProductAndProvidedExpirationDate_ShouldReturnConflict() throws Exception {

                BatchCreate batchCreate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "20000002"
                );

                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                String jsonRequest = objectMapper.writeValueAsString(batchCreate);

                mockMvc.perform(post("/api/batches")
                        .param("invoiceId", invoiceId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isConflict());
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testCreateBatch_WithNonPendingInvoice_ShouldReturnConflict() throws Exception {

                BatchCreate batchUpdate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "20000001"
                );

                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000002");

                String jsonRequest = objectMapper.writeValueAsString(batchUpdate);

                mockMvc.perform(post("/api/batches")
                        .param("invoiceId", invoiceId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isConflict());
            }
        }
    }

    // == Pruebas para PUT /api/batches/{batchId} == //

    @Nested
    @DisplayName("PUT /api/batches/{batchId}")
    class UpdateBatchTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testUpdateBatch_ValidDataAndBelongsToUser_shouldSucceed() throws Exception {

                BatchCreate batchUpdate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "20000001"
                );

                UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                String jsonRequest = objectMapper.writeValueAsString(batchUpdate);

                mockMvc.perform(put("/api/batches/"+batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.expirationDate").value(batchUpdate.expirationDate().toString()))
                        .andExpect(jsonPath("$.unitPrice").value(batchUpdate.unitPrice()))
                        .andExpect(jsonPath("$.quantity").value(batchUpdate.quantity()))
                        .andExpect(jsonPath("$.productId").value("00000000-0000-0000-0000-000000000001"))
                        .andExpect(jsonPath("$.productName").value("Leche Entera"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testUpdateBatchAndProductInfo_ValidDataAndBelongsToUser_shouldSucceed() throws Exception {

                BatchCreate batchUpdate = new BatchCreate(
                    null,
                    new BigDecimal("5.99"),
                    10,
                    "20000002"
                );

                UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                String jsonRequest = objectMapper.writeValueAsString(batchUpdate);

                mockMvc.perform(put("/api/batches/" + batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.unitPrice").value(batchUpdate.unitPrice()))
                        .andExpect(jsonPath("$.quantity").value(batchUpdate.quantity()))
                        .andExpect(jsonPath("$.productId").value("00000000-0000-0000-0000-000000000002"))
                        .andExpect(jsonPath("$.productName").value("Pan de Molde"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testUpdateBatch_WithOpenFoodFactsProduct_shouldSucceed() throws Exception {
                Thread.sleep(2000);
                BatchCreate batchUpdate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "4716982022201"
                );

                UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                String jsonRequest = objectMapper.writeValueAsString(batchUpdate);

                mockMvc.perform(put("/api/batches/" + batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.unitPrice").value(batchUpdate.unitPrice()))
                        .andExpect(jsonPath("$.quantity").value(batchUpdate.quantity()))
                        .andExpect(jsonPath("$.expirationDate").value(batchUpdate.expirationDate().toString()))
                        .andExpect(jsonPath("$.productName").value("Choco Bom"));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testUpdateBatch_WithNonExistentProduct_shouldReturnBadRequest() throws Exception {

                BatchCreate batchUpdate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "200000099"
                );

                UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                String jsonRequest = objectMapper.writeValueAsString(batchUpdate);

                mockMvc.perform(put("/api/batches/" + batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testUpdateBatch_WithNonExistentBatch_shouldReturnNotFound() throws Exception {

                BatchCreate batchUpdate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "20000001"
                );

                UUID batchId = UUID.fromString("99000000-0000-0000-0000-000000000001");

                String jsonRequest = objectMapper.writeValueAsString(batchUpdate);

                mockMvc.perform(put("/api/batches/" + batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isNotFound());
            }

            @Test
            @WithUserDetails("admin2@expmatik.com")
            void testUpdateBatch_InvoiceNotOwned_shouldReturnForbidden() throws Exception {

                BatchCreate batchUpdate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "20000001"
                );

                UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000001"); 

                String jsonRequest = objectMapper.writeValueAsString(batchUpdate);

                mockMvc.perform(put("/api/batches/" + batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isForbidden());
            }

            @Test
            @WithUserDetails("repo@expmatik.com")
            void testUpdateBatch_WithUnauthorizedUser_shouldReturnForbidden() throws Exception {

                BatchCreate batchUpdate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "20000001"
                );

                UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000001"); 

                String jsonRequest = objectMapper.writeValueAsString(batchUpdate);

                mockMvc.perform(put("/api/batches/" + batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isForbidden());
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testUpdateBatch_WithPerishableProductAndMissingExpirationDate_shouldReturnConflict() throws Exception {

                BatchCreate batchUpdate = new BatchCreate(
                    null,
                    new BigDecimal("5.99"),
                    10,
                    "20000001"
                );

                UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                String jsonRequest = objectMapper.writeValueAsString(batchUpdate);

                mockMvc.perform(put("/api/batches/" + batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isConflict());
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testUpdateBatch_WithNonPerishableProductAndProvidedExpirationDate_shouldReturnConflict() throws Exception {

                BatchCreate batchUpdate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "20000002"
                );

                UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                String jsonRequest = objectMapper.writeValueAsString(batchUpdate);

                mockMvc.perform(put("/api/batches/" + batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isConflict());
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testUpdateBatch_WithNonPendingInvoice_shouldReturnConflict() throws Exception {

                BatchCreate batchUpdate = new BatchCreate(
                    LocalDate.now().plusDays(10),
                    new BigDecimal("5.99"),
                    10,
                    "20000001"
                );

                UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000003");

                String jsonRequest = objectMapper.writeValueAsString(batchUpdate);

                mockMvc.perform(put("/api/batches/" + batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                        .andExpect(status().isConflict());
            }
        }
    }

    // == Pruebas para DELETE /api/batches/{batchId} == //

    @Nested
    @DisplayName("DELETE /api/batches/{batchId}")
    class DeleteBatchTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testDeleteBatch_ValidBatch_shouldSucceed() throws Exception {
                UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(MockMvcRequestBuilders.delete("/api/batches/" + batchId))
                        .andExpect(status().isNoContent());
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testDeleteBatch_WithNonExistentBatch_shouldReturnNotFound() throws Exception {
                UUID batchId = UUID.fromString("99000000-0000-0000-0000-000000000001");

                mockMvc.perform(MockMvcRequestBuilders.delete("/api/batches/" + batchId))
                        .andExpect(status().isNotFound());
            }

            @Test
            @WithUserDetails("admin2@expmatik.com")
            void testDeleteBatch_NotOwnedBatch_shouldReturnForbidden() throws Exception {
                UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(MockMvcRequestBuilders.delete("/api/batches/" + batchId))
                        .andExpect(status().isForbidden());
            }

            @Test
            @WithUserDetails("repo@expmatik.com")
            void testDeleteBatch_WithUnauthorizedUser_shouldReturnForbidden() throws Exception {
                UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(MockMvcRequestBuilders.delete("/api/batches/" + batchId))
                        .andExpect(status().isForbidden());
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testDeleteBatch_WithNonPendingInvoice_shouldReturnConflict() throws Exception {
                UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000003");

                mockMvc.perform(MockMvcRequestBuilders.delete("/api/batches/" + batchId))
                        .andExpect(status().isConflict());
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            void testDeleteBatch_WithOnlyOneBatchInInvoice_shouldReturnConflict() throws Exception {
                UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(MockMvcRequestBuilders.delete("/api/batches/" + batchId))
                        .andExpect(status().isNoContent());

                batchId = UUID.fromString("00000000-0000-0000-0000-000000000002");

                mockMvc.perform(MockMvcRequestBuilders.delete("/api/batches/" + batchId))
                        .andExpect(status().isConflict());
            }
        }
    }

}
