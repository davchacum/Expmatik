package com.expmatik.backend.sale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.expmatik.backend.sale.DTOs.SaleCreate;
import com.expmatik.backend.sale.DTOs.SaleRealTimeCreate;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SaleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // == Test Get /api/sales/{id} ==

    @Test
    @DisplayName("GET /api/sales/{id} - Success")
    @WithUserDetails("admin@expmatik.com")
    public void testGetSaleById_ValidId_ReturnsSale() throws Exception {

        UUID saleId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/sales/{id}", saleId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(saleId.toString()))
            .andExpect(jsonPath("$.totalAmount").value(2.50))
            .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/sales/{id} - Not Found")
    @WithUserDetails("admin@expmatik.com")
    public void testGetSaleById_InvalidId_ReturnsNotFound() throws Exception {

        UUID saleId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        mockMvc.perform(get("/api/sales/{id}", saleId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/sales/{id} - Access Denied for Unauthorized User")
    @WithUserDetails("repo@expmatik.com")
    public void testGetSaleById_UnauthorizedUser_ReturnsForbidden() throws Exception {

        UUID saleId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/sales/{id}", saleId))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/sales/{id} - Access Denied sale does not belong to user")
    @WithUserDetails("admin2@expmatik.com")
    public void testGetSaleById_SaleDoesNotBelongToUser_ReturnsForbidden() throws Exception {

        UUID saleId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/sales/{id}", saleId))
            .andExpect(status().isForbidden());
    }

    // == Test POST /api/sales ==

    @Test
    @DisplayName("POST /api/sales - Success")
    @WithUserDetails("admin@expmatik.com")
    public void testCreateSale_ValidRequest_ReturnsCreatedSale() throws Exception {
        SaleCreate saleCreate = new SaleCreate(
            LocalDateTime.now(),
            new BigDecimal("2.50"),
            PaymentMethod.CREDIT_CARD,
            TransactionStatus.SUCCESS,
            "20000001",
            "Máquina 1",
            1,
            1
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/sales")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(saleCreate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalAmount").value(2.50))
            .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /api/sales - Access Denied for Unauthorized User")
    @WithUserDetails("repo@expmatik.com")
    public void testCreateSale_UnauthorizedUser_ReturnsForbidden() throws Exception {
        SaleCreate saleCreate = new SaleCreate(
            LocalDateTime.now(),
            new BigDecimal("2.50"),
            PaymentMethod.CREDIT_CARD,
            TransactionStatus.SUCCESS,
            "20000001",
                        "Máquina 1",
            1,
            1
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/sales")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(saleCreate)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/sales - Access Denied sale does not belong to user")
    @WithUserDetails("admin2@expmatik.com")
    public void testCreateSale_SaleDoesNotBelongToUser_ReturnsForbidden() throws Exception {
        SaleCreate saleCreate = new SaleCreate(
            LocalDateTime.now(),
            new BigDecimal("2.50"),
            PaymentMethod.CREDIT_CARD,
            TransactionStatus.SUCCESS,
            "20000001",
                        "Máquina 1",
            1,
            1
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/sales")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(saleCreate)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/sales - vending slot not found")
    @WithUserDetails("admin@expmatik.com")
    public void testCreateSale_VendingSlotNotFound_ReturnsNotFound() throws Exception {
        SaleCreate saleCreate = new SaleCreate(
            LocalDateTime.now(),
            new BigDecimal("2.50"),
            PaymentMethod.CREDIT_CARD,
            TransactionStatus.SUCCESS,
            "20000001",
                        "Máquina 99",
            1,
            1
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/sales")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(saleCreate)))
            .andExpect(status().isNotFound());
    }

    // == Test POST /api/sales/real-time ==

    @Test
    @DisplayName("POST /api/sales/real-time - valid ID, should create a successful sale and decrement stock")
    @WithUserDetails("admin@expmatik.com")
    public void testDecrementVendingSlotStock_ValidIdAndValidQuantityAndAuthorizedUser_ShouldDecrementStock() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        String barcode = "20000001";
        SaleRealTimeCreate saleCreate = new SaleRealTimeCreate(
            PaymentMethod.CREDIT_CARD,
            vendingSlotId
            
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/sales/real-time")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(saleCreate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vendingSlotId").value(vendingSlotId.toString()))
            .andExpect(jsonPath("$.barcode").value(barcode))
            .andExpect(jsonPath("$.status").value("SUCCESS"));;
    }

    @Test
    @DisplayName("POST /api/vending-slots/{id}/decrement-stock - valid ID, valid quantity and authorized user should decrement stock and delete expiration batch if stock reaches zero")
    @WithUserDetails("admin@expmatik.com")
    public void testDecrementVendingSlotStock_ValidIdAndValidQuantityAndAuthorizedUser_ShouldDecrementStockAndDeleteExpirationBatchIfStockReachesZero() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000007");
        String barcode = "20000001";
        SaleRealTimeCreate saleCreate = new SaleRealTimeCreate(
            PaymentMethod.CREDIT_CARD,
            vendingSlotId
            
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/sales/real-time")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(saleCreate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vendingSlotId").value(vendingSlotId.toString()))
            .andExpect(jsonPath("$.barcode").value(barcode))
            .andExpect(jsonPath("$.status").value("SUCCESS"));;
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/decrement-stock - valid ID but not ADMINISTRATOR role should return 403")
    @WithUserDetails("repo@expmatik.com")
    public void testDecrementVendingSlotStock_ValidIdButNotAdministratorRole_ShouldReturn403() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        SaleRealTimeCreate saleCreate = new SaleRealTimeCreate(
            PaymentMethod.CREDIT_CARD,
            vendingSlotId
            
        );
        
        mockMvc.perform(MockMvcRequestBuilders.post("/api/sales/real-time")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(saleCreate)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/decrement-stock - valid ID but does not belong to the user should return 403")
    @WithUserDetails("admin2@expmatik.com")
    public void testDecrementVendingSlotStock_ValidIdButDoesNotBelongToUser_ShouldReturn403() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        SaleRealTimeCreate saleCreate = new SaleRealTimeCreate(
            PaymentMethod.CREDIT_CARD,
            vendingSlotId
            
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/sales/real-time")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(saleCreate)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/decrement-stock - non-existent vending slot should return 404")
    @WithUserDetails("admin@expmatik.com")
    public void testDecrementVendingSlotStock_NonExistentVendingSlot_ShouldReturn404() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        SaleRealTimeCreate saleCreate = new SaleRealTimeCreate(
            PaymentMethod.CREDIT_CARD,
            vendingSlotId
            
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/sales/real-time")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(saleCreate)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/sales/real-time - decrementing stock below zero should return a failed sale with reason OUT_OF_STOCK")
    @WithUserDetails("admin@expmatik.com")
    public void testDecrementVendingSlotStock_DecrementingBelowZero_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        SaleRealTimeCreate saleCreate = new SaleRealTimeCreate(
            PaymentMethod.CREDIT_CARD,
            vendingSlotId
            
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/sales/real-time")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(saleCreate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vendingSlotId").value(vendingSlotId.toString()))
            .andExpect(jsonPath("$.totalAmount").value(3))
            .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
            .andExpect(jsonPath("$.barcode").value("20000001"))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.failureReason").value("Cannot remove stock from the vending slot because it is empty."));
    }

    @Test
    @DisplayName("POST /api/sales/real-time - blocked slot should return failed sale with reason BLOCKED")
    @WithUserDetails("admin@expmatik.com")
    public void testDecrementVendingSlotStock_BlockedSlot_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000006");
        SaleRealTimeCreate saleCreate = new SaleRealTimeCreate(
            PaymentMethod.CREDIT_CARD,
            vendingSlotId
            
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/sales/real-time")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(saleCreate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vendingSlotId").value(vendingSlotId.toString()))
            .andExpect(jsonPath("$.totalAmount").value(3))
            .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
            .andExpect(jsonPath("$.barcode").value("20000001"))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.failureReason").value("The vending slot is blocked for maintenance."));
    }

    @Test
    @DisplayName("POST /api/sales/real-time - product not assigned to slot should return ConflictException")
    @WithUserDetails("admin@expmatik.com")
    public void testDecrementVendingSlotStock_ProductNotAssigned_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000005");
        SaleRealTimeCreate saleCreate = new SaleRealTimeCreate(
            PaymentMethod.CREDIT_CARD,
            vendingSlotId
            
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/sales/real-time")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(saleCreate)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/sales/real-time - productInfo needUpdate should Throw ConflictException")
    @WithUserDetails("admin@expmatik.com")
    public void testDecrementVendingSlotStock_ProductInfoNeedUpdate_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000008");
        SaleRealTimeCreate saleCreate = new SaleRealTimeCreate(
            PaymentMethod.CREDIT_CARD,
            vendingSlotId
            
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/sales/real-time")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(saleCreate)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/sales/real-time - expired products should return failed sale with reason EXPIRED_PRODUCTS")
    @WithUserDetails("admin@expmatik.com")
    public void testDecrementVendingSlotStock_ExpiredProducts_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        SaleRealTimeCreate saleCreate = new SaleRealTimeCreate(
            PaymentMethod.CREDIT_CARD,
            vendingSlotId
            
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/sales/real-time")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(saleCreate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vendingSlotId").value(vendingSlotId.toString()))
            .andExpect(jsonPath("$.totalAmount").value(3))
            .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
            .andExpect(jsonPath("$.barcode").value("20000001"))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.failureReason").value("Cannot register sale because the product is expired."));
    }

    // == Get /api/sales ==

    @Test
    @DisplayName("GET /api/sales - should return page of sales for the authenticated user")
    @WithUserDetails("admin@expmatik.com")
    public void testGetSales_ForAuthenticatedUser_ShouldReturnPageOfSales() throws Exception {
        mockMvc.perform(get("/api/sales"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(2));

    }

    @Test
    @DisplayName("GET /api/sales - should return empty page if user has no sales")
    @WithUserDetails("admin2@expmatik.com")
    public void testGetSales_ForAuthenticatedUserWithNoSales_ShouldReturnEmptyPage() throws Exception {
        mockMvc.perform(get("/api/sales"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(0));

    }

    @Test
    @DisplayName("GET /api/sales - should return 403 Forbidden for unauthorized user")
    @WithUserDetails("repo@expmatik.com")
    public void testGetSales_ForUnauthorizedUser_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/sales"))
            .andExpect(status().isForbidden());
    }

}