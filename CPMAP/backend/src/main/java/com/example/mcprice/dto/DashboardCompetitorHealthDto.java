package com.example.mcprice.dto;

import java.util.Map;

public record DashboardCompetitorHealthDto(
        Map<String, Double> crawlSuccessRateByCompetitor,
        Map<String, Long> confirmedListingCountByCompetitor,
        Map<String, Double> dailyCrawlSuccessRate,
        Map<String, String> lastErrorByCompetitor
) {
}
