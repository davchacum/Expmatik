package com.expmatik.backend.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.expmatik.backend.analytics.DTOs.AnalyticsItem;
import com.expmatik.backend.analytics.DTOs.AnalyticsResponse;
import com.expmatik.backend.invoice.InvoiceStatus;
import com.expmatik.backend.sale.TransactionStatus;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private UUID userId;
    private UUID machineId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String productName;
    private String brand;
    private Pageable pageable;

    @BeforeEach
    public void setUp() {
        userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        machineId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        startDate = LocalDate.of(2026, 1, 1);
        endDate = LocalDate.of(2026, 3, 31);
        startDateTime = startDate.atStartOfDay();
        endDateTime = endDate.atTime(LocalTime.MAX);
        productName = null;
        brand = null;
        pageable = PageRequest.of(0, 10);
    }

    // == getAnalytics EXPENSES tests ==

    @Nested
    @DisplayName("getAnalytics EXPENSES")
    class GetAnalyticsExpensesTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("getAnalytics should return expenses analytics with total and breakdown")
            void testGetAnalytics_Expenses_shouldReturnExpensesAnalytics() {
                BigDecimal expectedTotal = new BigDecimal("150.00");
                Object[] row1 = new Object[]{"Product A", new BigDecimal("100.00"), 10L};
                Object[] row2 = new Object[]{"Product B", new BigDecimal("50.00"), 5L};
                Page<Object[]> expectedBreakdown = new PageImpl<>(List.of(row1, row2));

                when(analyticsRepository.getExpensesTotal(userId, startDate, endDate, InvoiceStatus.RECEIVED))
                    .thenReturn(expectedTotal);
                when(analyticsRepository.getExpensesBreakdownByProduct(userId, startDate, endDate, InvoiceStatus.RECEIVED, productName, brand, pageable))
                    .thenReturn(expectedBreakdown);

                AnalyticsResponse response = analyticsService.getAnalytics(
                    AnalyticsType.EXPENSES, startDate, endDate, machineId, userId, productName, brand, pageable
                );

                assertNotNull(response);
                assertEquals(expectedTotal, response.total());
                assertEquals(2, response.analyticsItem().getTotalElements());
            }

            @Test
            @DisplayName("getAnalytics should return zero total when repository returns null for expenses")
            void testGetAnalytics_Expenses_NullTotal_shouldReturnZero() {
                Page<Object[]> emptyBreakdown = new PageImpl<>(List.of());

                when(analyticsRepository.getExpensesTotal(userId, startDate, endDate, InvoiceStatus.RECEIVED))
                    .thenReturn(null);
                when(analyticsRepository.getExpensesBreakdownByProduct(userId, startDate, endDate, InvoiceStatus.RECEIVED, productName, brand, pageable))
                    .thenReturn(emptyBreakdown);

                AnalyticsResponse response = analyticsService.getAnalytics(
                    AnalyticsType.EXPENSES, startDate, endDate, machineId, userId, productName, brand, pageable
                );

                assertNotNull(response);
                assertEquals(BigDecimal.ZERO, response.total());
            }

            @Test
            @DisplayName("getAnalytics should map breakdown rows to AnalyticsItem correctly for expenses")
            void testGetAnalytics_Expenses_shouldMapBreakdownRowsCorrectly() {
                BigDecimal total = new BigDecimal("100.00");
                Object[] row = new Object[]{"Product A", new BigDecimal("100.00"), 10L};
                Page<Object[]> breakdown = new PageImpl<>(List.<Object[]>of(row));

                when(analyticsRepository.getExpensesTotal(userId, startDate, endDate, InvoiceStatus.RECEIVED))
                    .thenReturn(total);
                when(analyticsRepository.getExpensesBreakdownByProduct(userId, startDate, endDate, InvoiceStatus.RECEIVED, productName, brand, pageable))
                    .thenReturn(breakdown);

                AnalyticsResponse response = analyticsService.getAnalytics(
                    AnalyticsType.EXPENSES, startDate, endDate, machineId, userId, productName, brand, pageable
                );

                AnalyticsItem item = response.analyticsItem().getContent().get(0);
                assertEquals("Product A", item.label());
                assertEquals(new BigDecimal("100.00"), item.amount());
                assertEquals(10L, item.count());
            }
        }
    }

    // == getAnalytics INCOME tests ==

    @Nested
    @DisplayName("getAnalytics INCOME")
    class GetAnalyticsIncomeTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("getAnalytics should return income analytics with total and breakdown by machine")
            void testGetAnalytics_Income_shouldReturnIncomeAnalytics() {
                BigDecimal expectedTotal = new BigDecimal("500.00");
                Object[] row1 = new Object[]{"Machine A", new BigDecimal("300.00"), 30L};
                Object[] row2 = new Object[]{"Machine B", new BigDecimal("200.00"), 20L};
                Page<Object[]> expectedBreakdown = new PageImpl<>(List.of(row1, row2));

                when(analyticsRepository.getIncomeTotal(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId))
                    .thenReturn(expectedTotal);
                when(analyticsRepository.getIncomeBreakdownByMachine(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId, pageable))
                    .thenReturn(expectedBreakdown);

                AnalyticsResponse response = analyticsService.getAnalytics(
                    AnalyticsType.INCOME, startDate, endDate, machineId, userId, productName, brand, pageable
                );

                assertNotNull(response);
                assertEquals(expectedTotal, response.total());
                assertEquals(2, response.analyticsItem().getTotalElements());
            }

            @Test
            @DisplayName("getAnalytics should return zero total when repository returns null for income")
            void testGetAnalytics_Income_NullTotal_shouldReturnZero() {
                Page<Object[]> emptyBreakdown = new PageImpl<>(List.of());

                when(analyticsRepository.getIncomeTotal(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId))
                    .thenReturn(null);
                when(analyticsRepository.getIncomeBreakdownByMachine(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId, pageable))
                    .thenReturn(emptyBreakdown);

                AnalyticsResponse response = analyticsService.getAnalytics(
                    AnalyticsType.INCOME, startDate, endDate, machineId, userId, productName, brand, pageable
                );

                assertNotNull(response);
                assertEquals(BigDecimal.ZERO, response.total());
            }

            @Test
            @DisplayName("getAnalytics should map breakdown rows to AnalyticsItem correctly for income")
            void testGetAnalytics_Income_shouldMapBreakdownRowsCorrectly() {
                BigDecimal total = new BigDecimal("300.00");
                Object[] row = new Object[]{"Machine A", new BigDecimal("300.00"), 30L};
                Page<Object[]> breakdown = new PageImpl<>(List.<Object[]>of(row));

                when(analyticsRepository.getIncomeTotal(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId))
                    .thenReturn(total);
                when(analyticsRepository.getIncomeBreakdownByMachine(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId, pageable))
                    .thenReturn(breakdown);

                AnalyticsResponse response = analyticsService.getAnalytics(
                    AnalyticsType.INCOME, startDate, endDate, machineId, userId, productName, brand, pageable
                );

                AnalyticsItem item = response.analyticsItem().getContent().get(0);
                assertEquals("Machine A", item.label());
                assertEquals(new BigDecimal("300.00"), item.amount());
                assertEquals(30L, item.count());
            }
        }
    }

    // == getAnalytics INCOME_PRODUCT tests ==

    @Nested
    @DisplayName("getAnalytics INCOME_PRODUCT")
    class GetAnalyticsIncomeProductTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("getAnalytics should return income product analytics with total and breakdown by product")
            void testGetAnalytics_IncomeProduct_shouldReturnIncomeProductAnalytics() {
                BigDecimal expectedTotal = new BigDecimal("500.00");
                Object[] row1 = new Object[]{"Product A", new BigDecimal("300.00"), 30L};
                Object[] row2 = new Object[]{"Product B", new BigDecimal("200.00"), 20L};
                Page<Object[]> expectedBreakdown = new PageImpl<>(List.of(row1, row2));

                when(analyticsRepository.getIncomeTotal(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId))
                    .thenReturn(expectedTotal);
                when(analyticsRepository.getIncomeBreakdownByProduct(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId, productName, brand, pageable))
                    .thenReturn(expectedBreakdown);

                AnalyticsResponse response = analyticsService.getAnalytics(
                    AnalyticsType.INCOME_PRODUCT, startDate, endDate, machineId, userId, productName, brand, pageable
                );

                assertNotNull(response);
                assertEquals(expectedTotal, response.total());
                assertEquals(2, response.analyticsItem().getTotalElements());
            }

            @Test
            @DisplayName("getAnalytics should return zero total when repository returns null for income product")
            void testGetAnalytics_IncomeProduct_NullTotal_shouldReturnZero() {
                Page<Object[]> emptyBreakdown = new PageImpl<>(List.of());

                when(analyticsRepository.getIncomeTotal(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId))
                    .thenReturn(null);
                when(analyticsRepository.getIncomeBreakdownByProduct(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId, productName, brand, pageable))
                    .thenReturn(emptyBreakdown);

                AnalyticsResponse response = analyticsService.getAnalytics(
                    AnalyticsType.INCOME_PRODUCT, startDate, endDate, machineId, userId, productName, brand, pageable
                );

                assertNotNull(response);
                assertEquals(BigDecimal.ZERO, response.total());
            }

            @Test
            @DisplayName("getAnalytics should map breakdown rows to AnalyticsItem correctly for income by product")
            void testGetAnalytics_IncomeProduct_shouldMapBreakdownRowsCorrectly() {
                BigDecimal total = new BigDecimal("300.00");
                Object[] row = new Object[]{"Product A", new BigDecimal("300.00"), 30L};
                Page<Object[]> breakdown = new PageImpl<>(List.<Object[]>of(row));

                when(analyticsRepository.getIncomeTotal(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId))
                    .thenReturn(total);
                when(analyticsRepository.getIncomeBreakdownByProduct(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId, productName, brand, pageable))
                    .thenReturn(breakdown);

                AnalyticsResponse response = analyticsService.getAnalytics(
                    AnalyticsType.INCOME_PRODUCT, startDate, endDate, machineId, userId, productName, brand, pageable
                );

                AnalyticsItem item = response.analyticsItem().getContent().get(0);
                assertEquals("Product A", item.label());
                assertEquals(new BigDecimal("300.00"), item.amount());
                assertEquals(30L, item.count());
            }
        }
    }

    // == getAnalytics PROFIT tests ==

    @Nested
    @DisplayName("getAnalytics PROFIT")
    class GetAnalyticsProfitTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("getAnalytics should return positive profit when income exceeds expenses")
            void testGetAnalytics_Profit_PositiveProfit_shouldReturnPositiveTotal() {
                BigDecimal incomeTotal = new BigDecimal("500.00");
                BigDecimal expensesTotal = new BigDecimal("300.00");

                Object[] incomeRow = new Object[]{"Product A", new BigDecimal("500.00"), 50L};
                Object[] expensesRow = new Object[]{"Product A", new BigDecimal("300.00"), 30L};

                when(analyticsRepository.getIncomeTotal(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId))
                    .thenReturn(incomeTotal);
                when(analyticsRepository.getExpensesTotal(userId, startDate, endDate, InvoiceStatus.RECEIVED))
                    .thenReturn(expensesTotal);
                when(analyticsRepository.getIncomeBreakdownByProduct(eq(userId), eq(startDateTime), eq(endDateTime), eq(TransactionStatus.SUCCESS), eq(machineId), eq(productName), eq(brand), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.<Object[]>of(incomeRow)));
                when(analyticsRepository.getExpensesBreakdownByProduct(userId, startDate, endDate, InvoiceStatus.RECEIVED, productName, brand, pageable))
                    .thenReturn(new PageImpl<>(List.<Object[]>of(expensesRow)));

                AnalyticsResponse response = analyticsService.getAnalytics(
                    AnalyticsType.PROFIT, startDate, endDate, machineId, userId, productName, brand, pageable
                );

                assertNotNull(response);
                assertEquals(incomeTotal.subtract(expensesTotal), response.total());
            }

            @Test
            @DisplayName("getAnalytics should return negative profit when expenses exceed income")
            void testGetAnalytics_Profit_NegativeProfit_shouldReturnNegativeTotal() {
                BigDecimal incomeTotal = new BigDecimal("100.00");
                BigDecimal expensesTotal = new BigDecimal("300.00");

                Object[] incomeRow = new Object[]{"Product A", new BigDecimal("100.00"), 10L};
                Object[] expensesRow = new Object[]{"Product A", new BigDecimal("300.00"), 30L};

                when(analyticsRepository.getIncomeTotal(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId))
                    .thenReturn(incomeTotal);
                when(analyticsRepository.getExpensesTotal(userId, startDate, endDate, InvoiceStatus.RECEIVED))
                    .thenReturn(expensesTotal);
                when(analyticsRepository.getIncomeBreakdownByProduct(eq(userId), eq(startDateTime), eq(endDateTime), eq(TransactionStatus.SUCCESS), eq(machineId), eq(productName), eq(brand), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.<Object[]>of(incomeRow)));
                when(analyticsRepository.getExpensesBreakdownByProduct(userId, startDate, endDate, InvoiceStatus.RECEIVED, productName, brand, pageable))
                    .thenReturn(new PageImpl<>(List.<Object[]>of(expensesRow)));

                AnalyticsResponse response = analyticsService.getAnalytics(
                    AnalyticsType.PROFIT, startDate, endDate, machineId, userId, productName, brand, pageable
                );

                assertNotNull(response);
                assertEquals(incomeTotal.subtract(expensesTotal), response.total());
            }

            @Test
            @DisplayName("getAnalytics should return zero total when both income and expenses are null")
            void testGetAnalytics_Profit_BothTotalsNull_shouldReturnZero() {
                when(analyticsRepository.getIncomeTotal(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId))
                    .thenReturn(null);
                when(analyticsRepository.getExpensesTotal(userId, startDate, endDate, InvoiceStatus.RECEIVED))
                    .thenReturn(null);
                when(analyticsRepository.getIncomeBreakdownByProduct(eq(userId), eq(startDateTime), eq(endDateTime), eq(TransactionStatus.SUCCESS), eq(machineId), eq(productName), eq(brand), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));
                when(analyticsRepository.getExpensesBreakdownByProduct(userId, startDate, endDate, InvoiceStatus.RECEIVED, productName, brand, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

                AnalyticsResponse response = analyticsService.getAnalytics(
                    AnalyticsType.PROFIT, startDate, endDate, machineId, userId, productName, brand, pageable
                );

                assertNotNull(response);
                assertEquals(BigDecimal.ZERO, response.total());
            }

            @Test
            @DisplayName("getAnalytics should use zero income for products not present in income breakdown")
            void testGetAnalytics_Profit_ProductNotInIncome_shouldUseZeroAsIncome() {
                BigDecimal incomeTotal = new BigDecimal("500.00");
                BigDecimal expensesTotal = new BigDecimal("300.00");

                Object[] incomeRow = new Object[]{"Product B", new BigDecimal("500.00"), 50L};
                Object[] expensesRow = new Object[]{"Product A", new BigDecimal("300.00"), 30L};

                when(analyticsRepository.getIncomeTotal(userId, startDateTime, endDateTime, TransactionStatus.SUCCESS, machineId))
                    .thenReturn(incomeTotal);
                when(analyticsRepository.getExpensesTotal(userId, startDate, endDate, InvoiceStatus.RECEIVED))
                    .thenReturn(expensesTotal);
                when(analyticsRepository.getIncomeBreakdownByProduct(eq(userId), eq(startDateTime), eq(endDateTime), eq(TransactionStatus.SUCCESS), eq(machineId), eq(productName), eq(brand), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.<Object[]>of(incomeRow)));
                when(analyticsRepository.getExpensesBreakdownByProduct(userId, startDate, endDate, InvoiceStatus.RECEIVED, productName, brand, pageable))
                    .thenReturn(new PageImpl<>(List.<Object[]>of(expensesRow)));

                AnalyticsResponse response = analyticsService.getAnalytics(
                    AnalyticsType.PROFIT, startDate, endDate, machineId, userId, productName, brand, pageable
                );

                assertNotNull(response);
                AnalyticsItem item = response.analyticsItem().getContent().get(0);
                assertEquals("Product A", item.label());
                assertEquals(new BigDecimal("-300.00"), item.amount());
                assertEquals(0L, item.count());
            }
        }
    }
}
