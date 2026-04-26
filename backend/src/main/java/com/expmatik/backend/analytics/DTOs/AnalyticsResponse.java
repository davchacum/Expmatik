package com.expmatik.backend.analytics.DTOs;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;

public record AnalyticsResponse(
    BigDecimal total,
    Page<AnalyticsItem> analyticsItem
) {}
