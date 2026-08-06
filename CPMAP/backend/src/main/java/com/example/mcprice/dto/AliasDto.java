package com.example.mcprice.dto;

import java.math.BigDecimal;

public record AliasDto(
        Long id,
        String aliasType,
        String aliasOriginal,
        String aliasNormalized,
        boolean confirmed,
        BigDecimal confidence
) {
}
