package com.example.mcprice.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ManualPriceRequest(
        @NotNull(message = "competitorListingId khong duoc de trong") Long competitorListingId,
        BigDecimal price,
        boolean contactOnly,
        String note
) {
}
