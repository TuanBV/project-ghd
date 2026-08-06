package com.example.mcprice.dto;

import jakarta.validation.constraints.NotBlank;

public record AliasCreateRequest(
        @NotBlank(message = "aliasOriginal khong duoc de trong") String aliasOriginal,
        @NotBlank(message = "aliasType khong duoc de trong") String aliasType,
        boolean confirmed
) {
}
