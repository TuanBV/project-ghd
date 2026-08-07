package com.example.mcprice.dto;

import java.util.List;

public record PublishRequest(List<Long> recommendationIds) {
}
