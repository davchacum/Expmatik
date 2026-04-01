package com.expmatik.backend.batch;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

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

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateBatch() throws Exception {

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
    void testCreateBatchAndProductInfo() throws Exception {

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
    void testCreateBatchWithOpenFoodFactsProduct() throws Exception {
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

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateBatchWithNonExistentProduct() throws Exception {

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
    void testCreateBatchWithNonExistentInvoice() throws Exception {

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
    void testCreateBatchWithUnauthorizedUser() throws Exception {

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
    void testCreateBatchWithPerishableProductAndMissingExpirationDate() throws Exception {

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
    void testCreateBatchWithNonPerishableProductAndProvidedExpirationDate() throws Exception {

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
    void testCreateBatchWithNonPendingInvoice() throws Exception {

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

    // == Pruebas para PUT /api/batches/{batchId} == //

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateBatch() throws Exception {

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
    void testUpdateBatchAndProductInfo() throws Exception {

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
    void testUpdateBatchWithOpenFoodFactsProduct() throws Exception {
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

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateBatchWithNonExistentProduct() throws Exception {

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
    void testUpdateBatchWithNonExistentBatch() throws Exception {

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
    void testUpdateBatchWithUnauthorizedUser() throws Exception {

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
    void testUpdateBatchWithPerishableProductAndMissingExpirationDate() throws Exception {

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
    void testUpdateBatchWithNonPerishableProductAndProvidedExpirationDate() throws Exception {

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
    void testUpdateBatchWithNonPendingInvoice() throws Exception {

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

    // == Pruebas para DELETE /api/batches/{batchId} == //

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testDeleteBatch() throws Exception {
        UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/batches/" + batchId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testDeleteBatchWithNonExistentBatch() throws Exception {
        UUID batchId = UUID.fromString("99000000-0000-0000-0000-000000000001");

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/batches/" + batchId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testDeleteBatchWithUnauthorizedUser() throws Exception {
        UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/batches/" + batchId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testDeleteBatchWithNonPendingInvoice() throws Exception {
        UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000003");

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/batches/" + batchId))
                .andExpect(status().isConflict());
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testDeleteBatchWithOnlyOneBatchInInvoice() throws Exception {
        UUID batchId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/batches/" + batchId))
                .andExpect(status().isNoContent());

        batchId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/batches/" + batchId))
                .andExpect(status().isConflict());
    }

}
