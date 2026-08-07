package com.example.mcprice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Body cho PATCH /api/recommendations/{id} — cap nhat trang thai duyet (thay cho /approve, /reject cu). */
public record UpdateRecommendationStatusRequest(
        @NotBlank(message = "status khong duoc de trong")
        @Pattern(regexp = "APPROVED|REJECTED", message = "status phai la APPROVED hoac REJECTED")
        String status,
        String reason
) {
}
