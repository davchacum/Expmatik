package com.expmatik.backend.productInfo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.productInfo.DTOs.ProductInfoUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ProductInfoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    //== Pruebas de GET /api/product-info ==//

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testGetAllProductInfoForUser() throws Exception {
        
        mockMvc.perform(get("/api/product-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productId").value("00000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$[1].productId").value("00000000-0000-0000-0000-000000000004"));
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testGetAllProductInfoForUser_WithOtherUserNoResults() throws Exception {
        mockMvc.perform(get("/api/product-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // == Prueba de Update /api/product-info/{id} == //

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateProductInfo() throws Exception {

        UUID productInfoId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        ProductInfoUpdate updateRequest = new ProductInfoUpdate(199, new BigDecimal("10"), new BigDecimal("0.15"));
        String jsonRequest = objectMapper.writeValueAsString(updateRequest);
        mockMvc.perform(put("/api/product-info/" + productInfoId)
                .contentType("application/json")
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(199))
                .andExpect(jsonPath("$.saleUnitPrice").value(10))
                .andExpect(jsonPath("$.vatRate").value(0.15));
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testUpdateProductInfo_Forbidden() throws Exception {

        UUID productInfoId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        ProductInfoUpdate updateRequest = new ProductInfoUpdate(199, new BigDecimal("10"), new BigDecimal("0.15"));
        String jsonRequest = objectMapper.writeValueAsString(updateRequest);
        mockMvc.perform(put("/api/product-info/" + productInfoId)
                .contentType("application/json")
                .content(jsonRequest))
                .andExpect(status().isForbidden());
    
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateProductInfo_NotFound() throws Exception {
        UUID productInfoId = UUID.fromString("99000000-0000-0000-0000-000000000001");

        ProductInfoUpdate updateRequest = new ProductInfoUpdate(199, new BigDecimal("10"), new BigDecimal("0.15"));
        String jsonRequest = objectMapper.writeValueAsString(updateRequest);
        mockMvc.perform(put("/api/product-info/" + productInfoId)
                .contentType("application/json")
                .content(jsonRequest))
                .andExpect(status().isNotFound());
    }

    // == Prueba de GET /api/product-info/get-or-create-product/{productId} == //

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testGetOrCreateProductInfo_OnlyGet() throws Exception {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.stockQuantity").value(200));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testGetOrCreateProductInfo_Create() throws Exception {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.stockQuantity").value(0));
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testGetOrCreateProductInfo_Forbidden() throws Exception {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000004");

        mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testGetOrCreateProductInfo_ProductNotFound() throws Exception {
        UUID productId = UUID.fromString("99000000-0000-0000-0000-000000000004");

        mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                .andExpect(status().isNotFound());
    }

    // == Prueba de PATCH /api/product-info/{id}/edit-stock == //

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testEditStockQuantity() throws Exception {
        UUID productInfoId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Integer newStockQuantity = 50;

        mockMvc.perform(patch("/api/product-info/" + productInfoId + "/edit-stock")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(newStockQuantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(150));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testEditStockQuantity_NegativeStock() throws Exception {
        UUID productInfoId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Integer newStockQuantity = -50;

        mockMvc.perform(patch("/api/product-info/" + productInfoId + "/edit-stock")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(newStockQuantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(50));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testEditStockQuantity_NegativeZeroStock() throws Exception {
        UUID productInfoId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Integer newStockQuantity = -200;

        mockMvc.perform(patch("/api/product-info/" + productInfoId + "/edit-stock")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(newStockQuantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(0));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testEditStockQuantity_NullStock() throws Exception {
        UUID productInfoId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(patch("/api/product-info/" + productInfoId + "/edit-stock")
                .contentType("application/json"))
                    .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testEditStockQuantity_Forbidden() throws Exception {
        UUID productInfoId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Integer newStockQuantity = 50;

        mockMvc.perform(patch("/api/product-info/" + productInfoId + "/edit-stock")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(newStockQuantity)))
                .andExpect(status().isForbidden());
    }
    



}
