package com.example.mcprice.dto;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        String title,
        String brand,
        String googleCategory,
        String productType,
        BigDecimal currentWebsitePrice,
        boolean active
) {
}
