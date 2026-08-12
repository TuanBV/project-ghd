package com.example.mcprice.controller;

import com.example.mcprice.dto.CompetitorListingDto;
import com.example.mcprice.dto.UpdateMatchStatusRequest;
import com.example.mcprice.exception.BusinessRuleException;
import com.example.mcprice.service.MatchConfirmPriceService;
import com.example.mcprice.service.MatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final MatchConfirmPriceService matchConfirmPriceService;

    /**
     * Cap nhat trang thai khop (thay cho /confirm, /reject cu — cung la sua field "status").
     * Khi xac nhan khop (MANUALLY_CONFIRMED): crawl gia THAT ngay cho listing nay + tinh lai
     * gia trung binh cho san pham, goi SAU KHI matchingService.confirm() da commit (tranh
     * CrawlItemExecutor doc DB truoc khi matchStatus moi duoc luu, cung nguyen tac autoDiscoverOnCreate).
     */
    @PatchMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public CompetitorListingDto updateStatus(@PathVariable Long productId, @PathVariable Long matchId,
                                              @Valid @RequestBody UpdateMatchStatusRequest request) {
        return switch (request.status()) {
            case "MANUALLY_CONFIRMED" -> matchConfirmPriceService.crawlPriceAndRecalculate(matchingService.confirm(productId, matchId));
            case "REJECTED" -> matchingService.reject(productId, matchId, request.reason());
            default -> throw new BusinessRuleException("status khong hop le: " + request.status());
        };
    }

    /** Nguoi dung bam "Lay gia moi nhat" tren 1 listing bat ky (khong can doi job tu dong) —
     * crawl lai URL do ngay va tinh lai gia trung binh cho san pham, dung chung logic voi luc xac nhan khop. */
    @PostMapping("/refresh-price")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public CompetitorListingDto refreshPrice(@PathVariable Long productId, @PathVariable Long matchId) {
        return matchConfirmPriceService.crawlPriceAndRecalculate(matchingService.getDtoOwnedByProduct(productId, matchId));
    }
}
