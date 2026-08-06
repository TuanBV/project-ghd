package com.example.mcprice.dto;

import java.math.BigDecimal;

public record ProductSummaryDto(
        Long id,
        String skuOriginal,
        String title,
        String productUrl,
        String brand,
        String googleCategory,
        String availability,
        BigDecimal currentWebsitePrice,
        BigDecimal currentMcPrice,
        String currency,
        long validCompetitorSourceCount,
        BigDecimal averageCompetitorPrice,
        BigDecimal suggestedPrice,
        String recommendationStatus,
        boolean hasMatchConflict
) {
}
