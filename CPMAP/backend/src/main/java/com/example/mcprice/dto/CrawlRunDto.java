package com.example.mcprice.dto;

import java.time.OffsetDateTime;

public record CrawlRunDto(
        Long id,
        String status,
        int totalItems,
        int successItems,
        int failedItems,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {
}
