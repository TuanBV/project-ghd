package com.example.mcprice.controller;

import com.example.mcprice.dto.PageResponse;
import com.example.mcprice.domain.PriceRecommendation;
import com.example.mcprice.domain.RecommendationStatus;
import com.example.mcprice.dto.ManualPriceRequest;
import com.example.mcprice.dto.OverrideRequest;
import com.example.mcprice.dto.PriceRecommendationDto;
import com.example.mcprice.dto.RejectRequest;
import com.example.mcprice.service.PriceCalculationService;
import com.example.mcprice.service.PricingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;
    private final PriceCalculationService priceCalculationService;

    @PostMapping("/api/pricing/recalculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public List<PriceRecommendationDto> recalculateAll() {
        return pricingService.recalculateAll();
    }

    @PostMapping("/api/pricing/products/{productId}/recalculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public PriceRecommendationDto recalculateProduct(@PathVariable Long productId) {
        return priceCalculationService.calculateForProduct(productId);
    }

    @PostMapping("/api/products/{productId}/manual-price")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    public void manualPrice(@PathVariable Long productId, @Valid @RequestBody ManualPriceRequest request) {
        pricingService.manualPrice(productId, request);
    }

    @GetMapping("/api/recommendations")
    public PageResponse<PriceRecommendationSummary> list(
            @RequestParam(required = false) RecommendationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        RecommendationStatus effective = status == null ? RecommendationStatus.REVIEW_REQUIRED : status;
        var result = pricingService.findByStatus(effective, PageRequest.of(page, size))
                .map(PricingController::toSummary);
        return PageResponse.of(result);
    }

    @PostMapping("/api/recommendations/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public PriceRecommendationDto approve(@PathVariable Long id) {
        return pricingService.approve(id);
    }

    @PostMapping("/api/recommendations/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public PriceRecommendationDto reject(@PathVariable Long id, @RequestBody(required = false) RejectRequest request) {
        return pricingService.reject(id, request == null ? null : request.reason());
    }

    @PostMapping("/api/recommendations/{id}/override")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public PriceRecommendationDto override(@PathVariable Long id, @Valid @RequestBody OverrideRequest request) {
        return pricingService.override(id, request);
    }

    private static PriceRecommendationSummary toSummary(PriceRecommendation r) {
        return new PriceRecommendationSummary(r.getId(), r.getProduct().getId(), r.getProduct().getTitle(),
                r.getCurrentPrice(), r.getFinalSuggestedPrice(), r.getStatus().name(), r.getIncludedSourceCount());
    }

    public record PriceRecommendationSummary(Long id, Long productId, String productTitle,
            java.math.BigDecimal currentPrice, java.math.BigDecimal finalSuggestedPrice, String status,
            int includedSourceCount) {
    }
}
