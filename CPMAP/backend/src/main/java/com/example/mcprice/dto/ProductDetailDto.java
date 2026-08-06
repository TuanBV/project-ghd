package com.example.mcprice.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailDto(
        Long id,
        String mcOfferId,
        String skuOriginal,
        String skuNormalized,
        String title,
        String description,
        String productUrl,
        String imageUrl,
        String brand,
        String googleCategory,
        String productType,
        String condition,
        String availability,
        BigDecimal currentWebsitePrice,
        BigDecimal currentMcPrice,
        String currency,
        boolean active,
        List<AliasDto> aliases,
        List<CompetitorListingDto> competitorListings,
        PriceRecommendationDto latestRecommendation
) {
}
