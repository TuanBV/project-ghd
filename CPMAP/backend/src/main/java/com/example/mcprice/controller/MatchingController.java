package com.example.mcprice.controller;

import com.example.mcprice.dto.CompetitorListingDto;
import com.example.mcprice.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/{productId}/matches/{matchId}")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    public record RejectRequest(String reason) {
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public CompetitorListingDto confirm(@PathVariable Long productId, @PathVariable Long matchId) {
        return matchingService.confirm(productId, matchId);
    }

    @PostMapping("/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public CompetitorListingDto reject(@PathVariable Long productId, @PathVariable Long matchId,
                                        @RequestBody(required = false) RejectRequest request) {
        return matchingService.reject(productId, matchId, request == null ? null : request.reason());
    }
}
