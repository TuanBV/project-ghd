package com.example.mcprice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PriceRecommendationDto(
        Long id,
        Long productId,
        String productTitle,
        BigDecimal currentPrice,
        BigDecimal rawAveragePrice,
        BigDecimal roundedPrice,
        BigDecimal finalSuggestedPrice,
        int includedSourceCount,
        int excludedSourceCount,
        String status,
        BigDecimal overridePrice,
        String overrideBy,
        String overrideReason,
        OffsetDateTime overrideExpiresAt,
        String approvedBy,
        OffsetDateTime approvedAt,
        OffsetDateTime createdAt,
        List<SourceBreakdown> sources
) {
    public record SourceBreakdown(
            Long observationId,
            Long competitorId,
            String competitorName,
            BigDecimal price,
            OffsetDateTime capturedAt,
            boolean included,
            String reason
    ) {
    }
}
