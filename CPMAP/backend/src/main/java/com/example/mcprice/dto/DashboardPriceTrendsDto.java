package com.example.mcprice.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardPriceTrendsDto(
        Map<String, Long> percentChangeDistribution,
        Map<String, CategoryPrice> currentVsSuggestedByCategory,
        List<TopMover> topIncreasing,
        List<TopMover> topDecreasing,
        Map<String, Long> categoriesWithMostMissingData
) {
    public record CategoryPrice(BigDecimal avgCurrentPrice, BigDecimal avgSuggestedPrice, long productCount) {
    }

    public record TopMover(Long productId, String title, BigDecimal currentPrice, BigDecimal suggestedPrice, BigDecimal percentChange) {
    }
}
