package com.example.mcprice.dto;

public record ImportIssueDto(
        Long id,
        Long importRowId,
        Integer rowNumber,
        String issueType,
        String severity,
        String message,
        boolean resolved
) {
}
