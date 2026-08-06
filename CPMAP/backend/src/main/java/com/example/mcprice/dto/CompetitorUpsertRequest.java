package com.example.mcprice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;

public record CompetitorUpsertRequest(
        @NotBlank(message = "name khong duoc de trong") String name,
        @NotBlank(message = "baseUrl khong duoc de trong") String baseUrl,
        boolean enabled,
        @NotNull(message = "crawlMode khong duoc de trong") String crawlMode,
        @Positive(message = "requestsPerMinute phai > 0") int requestsPerMinute,
        @Positive(message = "timeoutSeconds phai > 0") int timeoutSeconds,
        Map<String, Object> extractorConfig
) {
}
