package com.expmatik.backend.analytics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // == GET /api/analytics ==

    @Nested
    @DisplayName("GET /api/analytics")
    class GetAnalytics {

        @Nested
        @DisplayName("Success Cases Income")
        class SuccessCasesIncome {

            @Test
            @DisplayName("GET /api/analytics?type=INCOME - should return total income and breakdown by machine")
            @WithUserDetails("admin@expmatik.com")
            public void testGetAnalytics_Income_ReturnsIncomeAnalytics() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "INCOME"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(7.5))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(2));
            }

            @Test
            @DisplayName("GET /api/analytics?type=INCOME&machineId=... - should filter income by machine")
            @WithUserDetails("admin@expmatik.com")
            public void testGetAnalytics_Income_FilterByMachineId_ReturnsFilteredIncome() throws Exception {
                UUID machineId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(get("/api/analytics")
                    .param("type", "INCOME")
                    .param("machineId", machineId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(2.5))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(1))
                    .andExpect(jsonPath("$.analyticsItem.content[0].label").value("Máquina 1"))
                    .andExpect(jsonPath("$.analyticsItem.content[0].amount").value(2.5))
                    .andExpect(jsonPath("$.analyticsItem.content[0].count").value(1));
            }

            @Test
            @DisplayName("GET /api/analytics?type=INCOME&startDate=... - should return zero when no sales in date range")
            @WithUserDetails("admin@expmatik.com")
            public void testGetAnalytics_Income_OutOfRangeDates_ReturnsZero() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "INCOME")
                    .param("startDate", "2025-01-01")
                    .param("endDate", "2025-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(0));
            }

            @Test
            @DisplayName("GET /api/analytics?type=INCOME - user with no sales should return zero")
            @WithUserDetails("admin2@expmatik.com")
            public void testGetAnalytics_Income_UserWithNoSales_ReturnsZero() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "INCOME"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(0));
            }

        }

        @Nested
        @DisplayName("Success Cases Expenses")
        class SuccessCasesExpenses {

            @Test
            @DisplayName("GET /api/analytics?type=EXPENSES - should return total expenses and breakdown by product")
            @WithUserDetails("admin@expmatik.com")
            public void testGetAnalytics_Expenses_ReturnsExpensesAnalytics() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "EXPENSES"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(79.5))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(1))
                    .andExpect(jsonPath("$.analyticsItem.content[0].label").value("Leche Entera"))
                    .andExpect(jsonPath("$.analyticsItem.content[0].amount").value(79.5))
                    .andExpect(jsonPath("$.analyticsItem.content[0].count").value(50));
            }

            @Test
            @DisplayName("GET /api/analytics?type=EXPENSES&startDate=... - should return zero total when no invoices in date range")
            @WithUserDetails("admin@expmatik.com")
            public void testGetAnalytics_Expenses_OutOfRangeDates_ReturnsZero() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "EXPENSES")
                    .param("startDate", "2025-01-01")
                    .param("endDate", "2025-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(0));
            }

            @Test
            @DisplayName("GET /api/analytics?type=EXPENSES&productName=Leche - should filter breakdown by product name")
            @WithUserDetails("admin@expmatik.com")
            public void testGetAnalytics_Expenses_FilterByProductName_ReturnsFilteredBreakdown() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "EXPENSES")
                    .param("productName", "Leche"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(79.5))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(1))
                    .andExpect(jsonPath("$.analyticsItem.content[0].label").value("Leche Entera"));
            }

            @Test
            @DisplayName("GET /api/analytics?type=EXPENSES&productName=NoExiste - should return empty breakdown when product not found")
            @WithUserDetails("admin@expmatik.com")
            public void testGetAnalytics_Expenses_FilterByNonExistentProduct_ReturnsEmptyBreakdown() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "EXPENSES")
                    .param("productName", "NoExiste"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(0));
            }

            @Test
            @DisplayName("GET /api/analytics?type=EXPENSES - user with no invoices should return zero")
            @WithUserDetails("admin2@expmatik.com")
            public void testGetAnalytics_Expenses_UserWithNoInvoices_ReturnsZero() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "EXPENSES"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(0));
            }

        
        }

        @Nested
        @DisplayName("Success Cases Income Product")
        class SuccessCasesIncomeProduct {

            @Test
            @DisplayName("GET /api/analytics?type=INCOME_PRODUCT - should return total income and breakdown by product")
            @WithUserDetails("admin@expmatik.com")
            public void testGetAnalytics_IncomeProduct_ReturnsIncomeByProductAnalytics() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "INCOME_PRODUCT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(7.5))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(1))
                    .andExpect(jsonPath("$.analyticsItem.content[0].label").value("Leche Entera"))
                    .andExpect(jsonPath("$.analyticsItem.content[0].amount").value(7.5))
                    .andExpect(jsonPath("$.analyticsItem.content[0].count").value(2));
            }

            @Test
            @DisplayName("GET /api/analytics?type=INCOME_PRODUCT&productName=Leche - should filter income breakdown by product name")
            @WithUserDetails("admin@expmatik.com")
            public void testGetAnalytics_IncomeProduct_FilterByProductName_ReturnsFilteredBreakdown() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "INCOME_PRODUCT")
                    .param("productName", "Leche"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(7.5))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(1))
                    .andExpect(jsonPath("$.analyticsItem.content[0].label").value("Leche Entera"))
                    .andExpect(jsonPath("$.analyticsItem.content[0].amount").value(7.5))
                    .andExpect(jsonPath("$.analyticsItem.content[0].count").value(2));
            }

            @Test
            @DisplayName("GET /api/analytics?type=INCOME_PRODUCT&productName=NoExiste - should return empty breakdown when product not found")
            @WithUserDetails("admin@expmatik.com")
            public void testGetAnalytics_IncomeProduct_FilterByProductName_NotFound_ReturnsEmptyBreakdown() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "INCOME_PRODUCT")
                    .param("productName", "NoExiste"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(7.5))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(0));
            }

            @Test
            @DisplayName("GET /api/analytics?type=INCOME_PRODUCT - user with no sales should return zero")
            @WithUserDetails("admin2@expmatik.com")
            public void testGetAnalytics_IncomeProduct_NoSales_ReturnsZero() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "INCOME_PRODUCT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(0));
            }
        }

        @Nested
        @DisplayName("Success Cases Profit")
        class SuccessCasesProfit {

            @Test
            @DisplayName("GET /api/analytics?type=PROFIT - should return total profit (income - expenses) and breakdown per product")
            @WithUserDetails("admin@expmatik.com")
            public void testGetAnalytics_Profit_ReturnsProfitAnalytics() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "PROFIT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(-72.0))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(1))
                    .andExpect(jsonPath("$.analyticsItem.content[0].label").value("Leche Entera"))
                    .andExpect(jsonPath("$.analyticsItem.content[0].amount").value(-72.0))
                    .andExpect(jsonPath("$.analyticsItem.content[0].count").value(0));
            }

            @Test
            @DisplayName("GET /api/analytics?type=PROFIT&productName=Leche - should filter profit breakdown by product name")
            @WithUserDetails("admin@expmatik.com")
            public void testGetAnalytics_Profit_FilterByProductName() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "PROFIT")
                    .param("productName", "Leche"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(-72.0))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(1))
                    .andExpect(jsonPath("$.analyticsItem.content[0].label").value("Leche Entera"))
                    .andExpect(jsonPath("$.analyticsItem.content[0].amount").value(-72.0))
                    .andExpect(jsonPath("$.analyticsItem.content[0].count").value(0));
            }

            @Test
            @DisplayName("GET /api/analytics?type=PROFIT&productName=NoExiste - should return zero profit and empty breakdown when product not found")
            @WithUserDetails("admin@expmatik.com")
            public void testGetAnalytics_Profit_FilterByProductName_NotFound() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "PROFIT")
                    .param("productName", "NoExiste"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(-72))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(0));
            }

            @Test
            @DisplayName("GET /api/analytics?type=PROFIT - user with no sales or expenses should return zero profit and empty breakdown")
            @WithUserDetails("admin2@expmatik.com")
            public void testGetAnalytics_Profit_NoSalesOrExpenses() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "PROFIT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.analyticsItem.content").isArray())
                    .andExpect(jsonPath("$.analyticsItem.content.length()").value(0));
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("should return 403 Forbidden for non-ADMINISTRATOR user")
            @WithUserDetails("repo@expmatik.com")
            public void testGetAnalytics_NonAdminUser_ReturnsForbidden() throws Exception {
                mockMvc.perform(get("/api/analytics")
                    .param("type", "INCOME"))
                    .andExpect(status().isForbidden());
            }
        }
    }
}
