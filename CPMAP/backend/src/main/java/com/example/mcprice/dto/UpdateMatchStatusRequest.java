package com.example.mcprice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Body cho PATCH /api/products/{productId}/matches/{matchId} — thay cho /confirm, /reject cu. */
public record UpdateMatchStatusRequest(
        @NotBlank(message = "status khong duoc de trong")
        @Pattern(regexp = "MANUALLY_CONFIRMED|REJECTED", message = "status phai la MANUALLY_CONFIRMED hoac REJECTED")
        String status,
        String reason
) {
}
