package com.example.mcprice.dto;

import java.time.OffsetDateTime;

public record WebsitePublishRunDto(
        Long id,
        String status,
        boolean dryRun,
        int totalItems,
        int successItems,
        int failedItems,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {
}
