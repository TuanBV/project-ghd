package com.example.mcprice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OverrideRequest(
        @NotNull(message = "price khong duoc de trong") BigDecimal price,
        @NotBlank(message = "reason khong duoc de trong") String reason,
        @NotNull(message = "expiresAt khong duoc de trong") OffsetDateTime expiresAt
) {
}
