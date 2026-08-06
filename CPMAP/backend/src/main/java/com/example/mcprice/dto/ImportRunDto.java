package com.example.mcprice.dto;

import java.time.OffsetDateTime;

public record ImportRunDto(
        Long id,
        String importType,
        String fileName,
        String status,
        int totalRows,
        int successRows,
        int issueRows,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {
}
