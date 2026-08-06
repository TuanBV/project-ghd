package com.example.mcprice.dto;

import java.math.BigDecimal;

public record PricePolicyDto(
        Long id,
        String scope,
        String category,
        Long productId,
        int minimumCompetitorCount,
        int maxObservationAgeHours,
        BigDecimal roundingStep,
        BigDecimal maxIncreasePercent,
        BigDecimal maxDecreasePercent,
        BigDecimal outlierThresholdPercent,
        String outlierStrategy,
        boolean requireManualApproval,
        BigDecimal minimumAllowedPrice,
        BigDecimal maximumAllowedPrice,
        boolean autoPublishEnabled
) {
}
