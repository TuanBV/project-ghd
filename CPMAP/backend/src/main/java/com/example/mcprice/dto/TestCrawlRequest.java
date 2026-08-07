package com.example.mcprice.dto;

import jakarta.validation.constraints.NotBlank;

public record TestCrawlRequest(@NotBlank(message = "url khong duoc de trong") String url) {
}
