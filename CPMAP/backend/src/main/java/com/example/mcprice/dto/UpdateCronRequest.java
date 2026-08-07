package com.example.mcprice.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCronRequest(
        @NotBlank(message = "cron khong duoc de trong") String cron,
        @NotBlank(message = "timezone khong duoc de trong") String timezone
) {
}
