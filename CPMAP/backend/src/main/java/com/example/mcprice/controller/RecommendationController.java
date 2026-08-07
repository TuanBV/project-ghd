package com.example.mcprice.controller;

import com.example.mcprice.domain.PriceRecommendation;
import com.example.mcprice.domain.RecommendationStatus;
import com.example.mcprice.dto.OverrideRequest;
import com.example.mcprice.dto.PageResponse;
import com.example.mcprice.dto.PriceRecommendationDto;
import com.example.mcprice.dto.PriceRecommendationSummary;
import com.example.mcprice.dto.UpdateRecommendationStatusRequest;
import com.example.mcprice.exception.BusinessRuleException;
import com.example.mcprice.service.PricingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final PricingService pricingService;

    @GetMapping
    public PageResponse<PriceRecommendationSummary> list(
            @RequestParam(required = false) RecommendationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        RecommendationStatus effective = status == null ? RecommendationStatus.REVIEW_REQUIRED : status;
        var result = pricingService.findByStatus(effective, PageRequest.of(page, size)).map(RecommendationController::toSummary);
        return PageResponse.of(result);
    }

    /** Cap nhat trang thai duyet (thay cho /approve, /reject cu — cung la sua field "status"). */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public PriceRecommendationDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateRecommendationStatusRequest request) {
        return switch (request.status()) {
            case "APPROVED" -> pricingService.approve(id);
            case "REJECTED" -> pricingService.reject(id, request.reason());
            default -> throw new BusinessRuleException("status khong hop le: " + request.status());
        };
    }

    /** Ghi de gia (danh tu "override" — PUT set toan bo ban ghi override). */
    @PutMapping("/{id}/override")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public PriceRecommendationDto putOverride(@PathVariable Long id, @Valid @RequestBody OverrideRequest request) {
        return pricingService.override(id, request);
    }

    /** Tinh lai TOAN BO recommendation (tao lai hang loat). */
    @PostMapping("/recalculations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public List<PriceRecommendationDto> createRecalculation() {
        return pricingService.recalculateAll();
    }

    private static PriceRecommendationSummary toSummary(PriceRecommendation r) {
        return new PriceRecommendationSummary(r.getId(), r.getProduct().getId(), r.getProduct().getTitle(),
                r.getCurrentPrice(), r.getFinalSuggestedPrice(), r.getStatus().name(), r.getIncludedSourceCount());
    }
}
