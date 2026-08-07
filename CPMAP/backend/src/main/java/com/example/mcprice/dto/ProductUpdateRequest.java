package com.example.mcprice.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductUpdateRequest(
        @Size(max = 1000, message = "title toi da 1000 ky tu") String title,
        @Size(max = 255, message = "brand toi da 255 ky tu") String brand,
        @Size(max = 500, message = "googleCategory toi da 500 ky tu") String googleCategory,
        @Size(max = 500, message = "productType toi da 500 ky tu") String productType,
        @Positive(message = "currentWebsitePrice phai > 0") BigDecimal currentWebsitePrice,
        boolean active,
        @Pattern(regexp = "^(IN_STOCK|OUT_OF_STOCK|PREORDER|UNKNOWN)$", message = "availability khong hop le")
        String availability
) {
}
