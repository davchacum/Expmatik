package com.expmatik.backend.analytics.DTOs;

import java.math.BigDecimal;

public record AnalyticsItem(String label, BigDecimal amount, Long count) {}
