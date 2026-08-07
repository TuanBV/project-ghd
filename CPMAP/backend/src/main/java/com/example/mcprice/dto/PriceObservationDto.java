package com.example.mcprice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PriceObservationDto(
        Long id,
        Long competitorListingId,
        BigDecimal price,
        String currency,
        String availability,
        String sourceType,
        String observationStatus,
        String note,
        OffsetDateTime capturedAt
) {
}
