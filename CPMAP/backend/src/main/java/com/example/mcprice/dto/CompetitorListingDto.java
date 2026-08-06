package com.example.mcprice.dto;

import java.math.BigDecimal;

public record CompetitorListingDto(
        Long id,
        Long productId,
        String productTitle,
        String productSku,
        Long competitorId,
        String competitorName,
        String url,
        String externalSku,
        String matchMethod,
        BigDecimal matchScore,
        String matchReason,
        String matchStatus,
        boolean active
) {
}
