package com.example.mcprice.dto;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardSummaryDto(
        long totalMcProducts,
        long matchedProducts,
        long unmatchedProducts,
        long conflictProducts,
        Map<String, Long> productsBySourceCount,
        Map<String, Long> recommendationsByStatus,
        long priceIncreasedCount,
        long priceDecreasedCount,
        long priceUnchangedCount,
        BigDecimal totalPriceDifference,
        BigDecimal averagePriceDifference,
        Map<String, Double> crawlSuccessRateByCompetitor,
        long staleObservationCount,
        LastRunInfo lastJobRun,
        LastRunInfo lastJobError,
        long merchantSyncSuccessCount,
        long merchantSyncFailedCount
) {
    public record LastRunInfo(String jobKey, String status, String finishedAt) {
    }
}
