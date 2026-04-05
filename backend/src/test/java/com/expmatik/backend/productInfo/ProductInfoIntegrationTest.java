package com.expmatik.backend.productInfo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    @Nested
    @DisplayName("Tests for GET /api/product-info")
    class GetAllProductInfoForUser {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

                @Test
                @WithUserDetails("admin@expmatik.com")
                void testGetAllProductInfoForUser_ValidUser_ShouldReturnProductInfos() throws Exception {
                        
                        mockMvc.perform(get("/api/product-info"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content.length()").value(3))
                                .andExpect(jsonPath("$.content[0].productId").value("00000000-0000-0000-0000-000000000001"))
                                .andExpect(jsonPath("$.content[1].productId").value("00000000-0000-0000-0000-000000000005"))
                                .andExpect(jsonPath("$.content[2].productId").value("00000000-0000-0000-0000-000000000004"));
                }

                @Test
                @WithUserDetails("admin2@expmatik.com")
                void testGetAllProductInfoForUser_WithoutProductInfos_ShouldReturnEmptyList() throws Exception {
                        mockMvc.perform(get("/api/product-info"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isEmpty());
                }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

                @Test
                @WithUserDetails("repo@expmatik.com")
                void testGetAllProductInfoForUser_ForbiddenUser_ShouldReturnForbidden() throws Exception {
                        mockMvc.perform(get("/api/product-info"))
                                .andExpect(status().isForbidden());
                }
        }
    }

    // == Prueba de Update /api/product-info/{id} == //
    @Nested
    @DisplayName("Tests for PUT /api/product-info/{id}")
    class UpdateProductInfo {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

                @Test
                @WithUserDetails("admin@expmatik.com")
                @DisplayName("When data is valid, should update product info")
                void testUpdateProductInfo_ValidData_ShouldUpdateProductInfo() throws Exception {

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
                @WithUserDetails("admin@expmatik.com")
                @DisplayName("When inventory level is low, should create notification")
                void testUpdateProductInfo_StockLow_ShouldUpdateProductInfoAndCreateNotificationInventoryStockLow() throws Exception {

                        UUID productInfoId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                        ProductInfoUpdate updateRequest = new ProductInfoUpdate(5, new BigDecimal("10"), new BigDecimal("0.15"));
                        String jsonRequest = objectMapper.writeValueAsString(updateRequest);
                        mockMvc.perform(put("/api/product-info/" + productInfoId)
                                .contentType("application/json")
                                .content(jsonRequest))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.stockQuantity").value(5))
                                .andExpect(jsonPath("$.saleUnitPrice").value(10))
                                .andExpect(jsonPath("$.vatRate").value(0.15));

                        mockMvc.perform(get("/api/notifications"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].type").value("INVENTORY_STOCK_LOW"))
                                .andExpect(jsonPath("$.content[0].isRead").value(false))
                                .andExpect(jsonPath("$.content[0].createdAt").value(Matchers.startsWith(LocalDate.now().toString())));
                }

                @Test
                @WithUserDetails("admin@expmatik.com")
                @DisplayName("When inventory is out of stock, should create notification")
                void testUpdateProductInfo_OutOfStock_ShouldUpdateProductInfoAndCreateNotificationInventoryOutOfStock() throws Exception {

                        UUID productInfoId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                        ProductInfoUpdate updateRequest = new ProductInfoUpdate(0, new BigDecimal("10"), new BigDecimal("0.15"));
                        String jsonRequest = objectMapper.writeValueAsString(updateRequest);
                        mockMvc.perform(put("/api/product-info/" + productInfoId)
                                .contentType("application/json")
                                .content(jsonRequest))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.stockQuantity").value(0))
                                .andExpect(jsonPath("$.saleUnitPrice").value(10))
                                .andExpect(jsonPath("$.vatRate").value(0.15));

                        mockMvc.perform(get("/api/notifications"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].type").value("INVENTORY_OUT_OF_STOCK"))
                                .andExpect(jsonPath("$.content[0].isRead").value(false))
                                .andExpect(jsonPath("$.content[0].createdAt").value(Matchers.startsWith(LocalDate.now().toString())));
                }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

                @Test
                @WithUserDetails("admin2@expmatik.com")
                @DisplayName("When user is not authorized, should return forbidden")
                void testUpdateProductInfo_NotOwnedByUser_ShouldReturnForbidden() throws Exception {

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
                @DisplayName("When product info is not found, should return not found")
                void testUpdateProductInfo_InvalidId_ShouldReturnNotFound() throws Exception {
                        UUID productInfoId = UUID.fromString("99000000-0000-0000-0000-000000000001");

                        ProductInfoUpdate updateRequest = new ProductInfoUpdate(199, new BigDecimal("10"), new BigDecimal("0.15"));
                        String jsonRequest = objectMapper.writeValueAsString(updateRequest);
                        mockMvc.perform(put("/api/product-info/" + productInfoId)
                                .contentType("application/json")
                                .content(jsonRequest))
                                .andExpect(status().isNotFound());
                }

                @Test
                @WithUserDetails("repo@expmatik.com")
                @DisplayName("When user is forbidden, should return forbidden")
                void testUpdateProductInfo_ForbiddenUser_ShouldReturnForbidden() throws Exception {
                        UUID productInfoId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                        ProductInfoUpdate updateRequest = new ProductInfoUpdate(199, new BigDecimal("10"), new BigDecimal("0.15"));
                        String jsonRequest = objectMapper.writeValueAsString(updateRequest);
                        mockMvc.perform(put("/api/product-info/" + productInfoId)
                                .contentType("application/json")
                                .content(jsonRequest))
                                .andExpect(status().isForbidden());
                }
        }
    }

    // == Prueba de GET /api/product-info/get-or-create-product/{productId} == //
    @Nested
    @DisplayName("Tests for GET /api/product-info/get-or-create-product/{productId}")
    class GetOrCreateProductInfo {
        
        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

                @Test
                @WithUserDetails("admin@expmatik.com")
                void testGetOrCreateProductInfo_OnlyGet_shouldReturnProductInfo() throws Exception {
                        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                        mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.productId").value(productId.toString()))
                                .andExpect(jsonPath("$.stockQuantity").value(200));
                }

                @Test
                @WithUserDetails("admin@expmatik.com")
                void testGetOrCreateProductInfo_Create_shouldReturnCreatedProductInfo() throws Exception {
                        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000002");

                        mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.productId").value(productId.toString()))
                                .andExpect(jsonPath("$.stockQuantity").value(0));
                }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

                @Test
                @WithUserDetails("admin2@expmatik.com")
                void testGetOrCreateProductInfo_NotOwnedByUser_shouldReturnForbidden() throws Exception {
                        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000004");

                        mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                                .andExpect(status().isForbidden());
                }

                @Test
                @WithUserDetails("repo@expmatik.com")
                void testGetOrCreateProductInfo_AsMainTainer_shouldReturnForbidden() throws Exception {
                        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000004");

                        mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                                .andExpect(status().isForbidden());
                }

                @Test
                @WithUserDetails("admin@expmatik.com")
                void testGetOrCreateProductInfo_ProductNotFound_shouldReturnNotFound() throws Exception {
                        UUID productId = UUID.fromString("99000000-0000-0000-0000-000000000004");

                        mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                                .andExpect(status().isNotFound());
                }
        }
    }

    // == Prueba de PATCH /api/product-info/{id}/edit-stock == //

    @Nested
    @DisplayName("Tests for PATCH /api/product-info/{id}/edit-stock")
    class EditStockQuantity {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

                @Test
                @WithUserDetails("admin@expmatik.com")
                @DisplayName("When stock quantity is valid, should add stock quantity")
                void testEditStockQuantity_Valid_shouldReturnUpdatedProductInfoStock() throws Exception {
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
                @DisplayName("When stock quantity is negative, should add 0 to current stock quantity and return updated stock quantity")
                void testEditStockQuantity_NegativeStock_ShouldReturnUpdatedProductInfoStock() throws Exception {
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
                @DisplayName("When stock quantity is negative and results in zero, should return add 0 stock quantity")
                void testEditStockQuantity_NegativeZeroStock_ShouldReturnUpdatedProductInfoStock() throws Exception {
                        UUID productInfoId = UUID.fromString("00000000-0000-0000-0000-000000000001");
                        Integer newStockQuantity = -200;

                        mockMvc.perform(patch("/api/product-info/" + productInfoId + "/edit-stock")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(newStockQuantity)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.stockQuantity").value(0));
                }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

                @Test
                @WithUserDetails("admin@expmatik.com")
                @DisplayName("When stock quantity is null, should return bad request")
                void testEditStockQuantity_NullStock_ShouldReturnBadRequest() throws Exception {
                        UUID productInfoId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                        mockMvc.perform(patch("/api/product-info/" + productInfoId + "/edit-stock")
                                .contentType("application/json"))
                                .andExpect(status().isBadRequest());
                }

                @Test
                @WithUserDetails("admin2@expmatik.com")
                @DisplayName("When stock quantity is valid but user is not the owner, should return forbidden")
                void testEditStockQuantity_NotOwnedByUser_ShouldReturnForbidden() throws Exception {
                        UUID productInfoId = UUID.fromString("00000000-0000-0000-0000-000000000001");
                        Integer newStockQuantity = 50;

                        mockMvc.perform(patch("/api/product-info/" + productInfoId + "/edit-stock")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(newStockQuantity)))
                                .andExpect(status().isForbidden());
                }

                @Test
                @WithUserDetails("repo@expmatik.com")
                @DisplayName("When stock quantity is valid but user is a maintainer, should return forbidden")
                void testEditStockQuantity_AsMainTainer_shouldReturnForbidden() throws Exception {
                        UUID productInfoId = UUID.fromString("00000000-0000-0000-0000-000000000001");
                        Integer newStockQuantity = 50;

                        mockMvc.perform(patch("/api/product-info/" + productInfoId + "/edit-stock")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(newStockQuantity)))
                                .andExpect(status().isForbidden());
                }
        }

    }
}
