package com.example.mcprice.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        List<FieldViolation> fieldErrors
) {
    public record FieldViolation(String field, String message) {
    }
}
