package com.example.mcprice.dto;

import java.time.OffsetDateTime;

public record JobRunDto(
        Long id,
        String jobKey,
        String triggerType,
        String status,
        int totalItems,
        int successItems,
        int failedItems,
        int progressPercent,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String errorDetail,
        String correlationId
) {
}
