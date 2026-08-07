package com.example.mcprice.dto;

import java.math.BigDecimal;

public record PriceRecommendationSummary(
        Long id, Long productId, String productTitle,
        BigDecimal currentPrice, BigDecimal finalSuggestedPrice, String status,
        int includedSourceCount
) {
}
