package com.example.mcprice.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String username,
        String role,
        long expiresInSeconds
) {
}
