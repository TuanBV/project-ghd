package com.example.mcprice.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record CompetitorDto(
        Long id,
        String name,
        String baseUrl,
        boolean enabled,
        String crawlMode,
        int requestsPerMinute,
        int timeoutSeconds,
        Map<String, Object> extractorConfig,
        OffsetDateTime lastSuccessAt,
        OffsetDateTime lastErrorAt,
        String lastErrorMessage,
        Long lastDiscoveryJobRunId,
        String lastDiscoveryStatus,
        Integer lastDiscoveryProgressPercent,
        long discoveredUrlCount,
        long matchedProductCount,
        Long lastCrawlJobRunId,
        String lastCrawlStatus,
        Integer lastCrawlProgressPercent
) {
}
