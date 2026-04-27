package com.expmatik.backend.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.expmatik.backend.analytics.DTOs.AnalyticsItem;
import com.expmatik.backend.analytics.DTOs.AnalyticsResponse;
import com.expmatik.backend.invoice.InvoiceStatus;
import com.expmatik.backend.sale.TransactionStatus;

@Service
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    public AnalyticsResponse getAnalytics(AnalyticsType type, LocalDate startDate, LocalDate endDate,
                                           UUID machineId, UUID userId, String productName, String brand,
                                           Pageable pageable) {
        return switch (type) {
            case EXPENSES -> buildExpensesAnalytics(userId, startDate, endDate, productName, brand, pageable);
            case INCOME -> buildIncomeAnalytics(userId, machineId, startDate, endDate, pageable);
            case INCOME_PRODUCT -> buildIncomeProductAnalytics(userId, machineId, startDate, endDate, productName, brand, pageable);
            case PROFIT -> buildProfitAnalytics(userId, machineId, startDate, endDate, productName, brand, pageable);
        };
    }

    private AnalyticsResponse buildExpensesAnalytics(UUID userId, LocalDate startDate, LocalDate endDate,
                                                      String productName, String brand, Pageable pageable) {
        BigDecimal total = orZero(analyticsRepository.getExpensesTotal(userId, startDate, endDate, InvoiceStatus.RECEIVED));
        Page<AnalyticsItem> breakdown = analyticsRepository
            .getExpensesBreakdownByProduct(userId, startDate, endDate, InvoiceStatus.RECEIVED, productName, brand, pageable)
            .map(row -> new AnalyticsItem((String) row[0], toDecimal(row[1]), toLong(row[2])));
        return new AnalyticsResponse(total, breakdown);
    }

    private AnalyticsResponse buildIncomeAnalytics(UUID userId, UUID machineId, LocalDate startDate, LocalDate endDate,
                                                    Pageable pageable) {
        LocalDateTime start = toStartOfDay(startDate);
        LocalDateTime end = toEndOfDay(endDate);
        BigDecimal total = orZero(analyticsRepository.getIncomeTotal(userId, start, end, TransactionStatus.SUCCESS, machineId));
        Page<AnalyticsItem> breakdown = analyticsRepository
            .getIncomeBreakdownByMachine(userId, start, end, TransactionStatus.SUCCESS, machineId, pageable)
            .map(row -> new AnalyticsItem((String) row[0], toDecimal(row[1]), toLong(row[2])));
        return new AnalyticsResponse(total, breakdown);
    }

    private AnalyticsResponse buildIncomeProductAnalytics(UUID userId, UUID machineId, LocalDate startDate, LocalDate endDate,
                                                           String productName, String brand, Pageable pageable) {
        LocalDateTime start = toStartOfDay(startDate);
        LocalDateTime end = toEndOfDay(endDate);
        BigDecimal total = orZero(analyticsRepository.getIncomeTotal(userId, start, end, TransactionStatus.SUCCESS, machineId));
        Page<AnalyticsItem> breakdown = analyticsRepository
            .getIncomeBreakdownByProduct(userId, start, end, TransactionStatus.SUCCESS, machineId, productName, brand, pageable)
            .map(row -> new AnalyticsItem((String) row[0], toDecimal(row[1]), toLong(row[2])));
        return new AnalyticsResponse(total, breakdown);
    }

    private AnalyticsResponse buildProfitAnalytics(UUID userId, UUID machineId, LocalDate startDate, LocalDate endDate,
                                                    String productName, String brand, Pageable pageable) {
        LocalDateTime start = toStartOfDay(startDate);
        LocalDateTime end = toEndOfDay(endDate);
        BigDecimal totalIncome = orZero(analyticsRepository.getIncomeTotal(userId, start, end, TransactionStatus.SUCCESS, machineId));
        BigDecimal totalExpenses = orZero(analyticsRepository.getExpensesTotal(userId, startDate, endDate, InvoiceStatus.RECEIVED));
        BigDecimal total = totalIncome.subtract(totalExpenses);

        List<Object[]> incomeRows = analyticsRepository
            .getIncomeBreakdownByProduct(userId, start, end, TransactionStatus.SUCCESS, machineId, productName, brand, Pageable.unpaged())
            .getContent();

        Map<String, BigDecimal> incomeMap = incomeRows.stream()
            .collect(Collectors.toMap(r -> (String) r[0], r -> toDecimal(r[1])));

        Page<AnalyticsItem> breakdown = analyticsRepository
            .getExpensesBreakdownByProduct(userId, startDate, endDate, InvoiceStatus.RECEIVED, productName, brand, pageable)
            .map(r -> {
                String name = (String) r[0];
                BigDecimal profit = incomeMap.getOrDefault(name, BigDecimal.ZERO).subtract(toDecimal(r[1]));
                return new AnalyticsItem(name, profit, 0L);
            });

        return new AnalyticsResponse(total, breakdown);
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString());
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        return ((Number) value).longValue();
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date != null ? date.atTime(LocalTime.MAX) : null;
    }
}
