package com.example.mcprice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PricePolicyUpdateRequest(
        @Min(1) int minimumCompetitorCount,
        @Min(1) int maxObservationAgeHours,
        @NotNull BigDecimal roundingStep,
        @NotNull BigDecimal maxIncreasePercent,
        @NotNull BigDecimal maxDecreasePercent,
        @NotNull BigDecimal outlierThresholdPercent,
        @NotNull String outlierStrategy,
        boolean requireManualApproval,
        BigDecimal minimumAllowedPrice,
        BigDecimal maximumAllowedPrice,
        boolean autoPublishEnabled
) {
}
