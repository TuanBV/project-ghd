package com.example.mcprice.controller;

import com.example.mcprice.domain.PriceObservation;
import com.example.mcprice.dto.CompetitorListingDto;
import com.example.mcprice.dto.PageResponse;
import com.example.mcprice.dto.AliasCreateRequest;
import com.example.mcprice.dto.AliasDto;
import com.example.mcprice.dto.ManualPriceRequest;
import com.example.mcprice.dto.PriceObservationDto;
import com.example.mcprice.dto.PriceRecommendationDto;
import com.example.mcprice.dto.ProductDetailDto;
import com.example.mcprice.dto.ProductSummaryDto;
import com.example.mcprice.dto.ProductUpdateRequest;
import com.example.mcprice.service.MatchConfirmPriceService;
import com.example.mcprice.service.MatchingService;
import com.example.mcprice.service.PriceCalculationService;
import com.example.mcprice.service.PricingService;
import com.example.mcprice.service.ProductAliasService;
import com.example.mcprice.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductAliasService productAliasService;
    private final PricingService pricingService;
    private final PriceCalculationService priceCalculationService;
    private final MatchingService matchingService;
    private final MatchConfirmPriceService matchConfirmPriceService;

    @GetMapping
    public PageResponse<ProductSummaryDto> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) Long competitorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(productService.search(keyword, category, availability, competitorId, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ProductDetailDto getDetail(@PathVariable Long id) {
        return productService.getDetail(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ProductDetailDto update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return productService.update(id, request);
    }

    @PostMapping("/{id}/aliases")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public AliasDto createAlias(@PathVariable Long id, @Valid @RequestBody AliasCreateRequest request) {
        return productAliasService.create(id, request);
    }

    /** Nhap 1 ban ghi gia doi thu bang tay (tao moi 1 PriceObservation nguon MANUAL). */
    @PostMapping("/{id}/manual-prices")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    public PriceObservationDto createManualPrice(@PathVariable Long id, @Valid @RequestBody ManualPriceRequest request) {
        PriceObservation observation = pricingService.manualPrice(id, request);
        return toDto(observation);
    }

    /** Tinh lai (tao moi) recommendation cho 1 san pham cu the. */
    @PostMapping("/{id}/recommendations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public PriceRecommendationDto createRecommendation(@PathVariable Long id) {
        return priceCalculationService.calculateForProduct(id);
    }

    /**
     * Tim URL ung cu vien tren TOAN HE THONG theo SKU (khop SKU cua san pham dang gan hoac khop
     * chinh chuoi URL) — dung cho chuc nang "Tim theo SKU" o trang chi tiet san pham, giup tim
     * lai nhung URL da duoc kham pha nhung dang gan (co the nham) cho san pham khac.
     */
    @GetMapping("/{id}/listing-candidates")
    public List<CompetitorListingDto> listingCandidates(@PathVariable Long id, @RequestParam String sku) {
        return matchingService.searchCandidatesBySku(id, sku);
    }

    /**
     * Nhan (chuyen) 1 listing dang thuoc ve san pham khac ve san pham nay — dung khi nguoi dung
     * chon 1 URL ung cu vien tu ket qua "Tim theo SKU". Tinh lai gia cho CA san pham cu (vua mat
     * 1 nguon) lan san pham nay (vua co them 1 nguon, kem crawl gia that ngay).
     */
    @PutMapping("/{id}/listing-candidates/{listingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public CompetitorListingDto claimListingCandidate(@PathVariable Long id, @PathVariable Long listingId) {
        Long oldProductId = matchingService.getDto(listingId).productId();
        CompetitorListingDto reassigned = matchingService.reassignListing(listingId, id);
        CompetitorListingDto result = matchConfirmPriceService.crawlPriceAndRecalculate(reassigned);
        if (!oldProductId.equals(id)) {
            priceCalculationService.calculateForProduct(oldProductId);
        }
        return result;
    }

    private PriceObservationDto toDto(PriceObservation o) {
        return new PriceObservationDto(o.getId(), o.getCompetitorListing().getId(), o.getPrice(), o.getCurrency(),
                o.getAvailability(), o.getSourceType().name(), o.getObservationStatus().name(), o.getNote(), o.getCapturedAt());
    }
}
