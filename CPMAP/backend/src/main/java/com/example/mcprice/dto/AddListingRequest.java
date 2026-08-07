package com.example.mcprice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddListingRequest(
        @NotNull(message = "productId khong duoc de trong") Long productId,
        @NotBlank(message = "url khong duoc de trong") String url
) {
}
