package com.example.mcprice.dto;

import java.util.List;

public record RunCrawlRequest(List<Long> competitorIds) {
}
