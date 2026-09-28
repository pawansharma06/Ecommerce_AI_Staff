package com.shopai.order.dto;

import java.math.BigDecimal;

public record OrderStatsResponse(
        long totalOrders,
        long unfulfilledOrders,
        long partiallyFulfilledOrders,
        long fulfilledOrders,
        long paidOrders,
        long pendingOrders,
        BigDecimal totalSales,
        long abandonedCheckoutsCount,
        long recoveredCheckoutsCount,
        BigDecimal totalAbandonedValue
) {}
