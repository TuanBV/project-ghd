package com.example.mcprice.service;

import com.example.mcprice.exception.NotFoundException;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.domain.PriceRecommendation;
import com.example.mcprice.repository.PriceRecommendationRepository;
import com.example.mcprice.domain.Product;
import com.example.mcprice.dto.AliasDto;
import com.example.mcprice.dto.ProductDetailDto;
import com.example.mcprice.dto.ProductSummaryDto;
import com.example.mcprice.dto.ProductUpdateRequest;
import com.example.mcprice.repository.ProductAliasRepository;
import com.example.mcprice.repository.ProductRepository;
import com.example.mcprice.repository.ProductSpecifications;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private static final List<MatchStatus> CONFIRMED_STATUSES = List.of(MatchStatus.AUTO_CONFIRMED, MatchStatus.MANUALLY_CONFIRMED);

    private final ProductRepository productRepository;
    private final ProductAliasRepository productAliasRepository;
    private final CompetitorListingRepository competitorListingRepository;
    private final PriceRecommendationRepository priceRecommendationRepository;
    private final MatchingService matchingService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<ProductSummaryDto> search(String keyword, String category, String availability, Pageable pageable) {
        var spec = ProductSpecifications.allOf(
                ProductSpecifications.search(keyword),
                ProductSpecifications.category(category),
                ProductSpecifications.availability(availability));
        Page<Product> page = productRepository.findAll(spec, pageable);
        List<Long> productIds = page.getContent().stream().map(Product::getId).toList();

        List<PriceRecommendation> latestRecommendations = productIds.isEmpty()
                ? List.of() : priceRecommendationRepository.findLatestForProducts(productIds);
        Map<Long, PriceRecommendation> recommendationByProduct = latestRecommendations.stream()
                .collect(java.util.stream.Collectors.toMap(r -> r.getProduct().getId(), r -> r, (a, b) -> a));

        var listingsForPage = productIds.isEmpty() ? List.<com.example.mcprice.domain.CompetitorListing>of()
                : competitorListingRepository.findByProductIdIn(productIds);

        Map<Long, Long> confirmedCountByProduct = listingsForPage.stream()
                .filter(l -> l.isActive() && CONFIRMED_STATUSES.contains(l.getMatchStatus()))
                .collect(java.util.stream.Collectors.groupingBy(l -> l.getProduct().getId(), java.util.stream.Collectors.counting()));

        Map<Long, Boolean> conflictByProduct = listingsForPage.stream()
                .collect(java.util.stream.Collectors.groupingBy(l -> l.getProduct().getId(),
                        java.util.stream.Collectors.mapping(l -> l.getMatchStatus() == MatchStatus.REVIEW_REQUIRED, java.util.stream.Collectors.reducing(false, Boolean::logicalOr))));

        return page.map(p -> {
            PriceRecommendation rec = recommendationByProduct.get(p.getId());
            return new ProductSummaryDto(
                    p.getId(), p.getSkuOriginal(), p.getTitle(), p.getProductUrl(), p.getBrand(), p.getGoogleCategory(),
                    p.getAvailability(), p.getCurrentWebsitePrice(), p.getCurrentMcPrice(), p.getCurrency(),
                    confirmedCountByProduct.getOrDefault(p.getId(), 0L),
                    rec == null ? null : rec.getRawAveragePrice(),
                    rec == null ? null : rec.getFinalSuggestedPrice(),
                    rec == null ? null : rec.getStatus().name(),
                    conflictByProduct.getOrDefault(p.getId(), false));
        });
    }

    @Transactional(readOnly = true)
    public ProductDetailDto getDetail(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> NotFoundException.of("Product", id));
        List<AliasDto> aliases = productAliasRepository.findByProductId(id).stream()
                .map(a -> new AliasDto(a.getId(), a.getAliasType().name(), a.getAliasOriginal(), a.getAliasNormalized(),
                        a.isConfirmed(), a.getConfidence()))
                .toList();
        var listings = competitorListingRepository.findByProductId(id).stream().map(matchingService::toDto).toList();
        PriceRecommendation latest = priceRecommendationRepository.findFirstByProductIdOrderByCreatedAtDesc(id).orElse(null);
        var recommendationDto = latest == null ? null : new com.example.mcprice.dto.PriceRecommendationDto(
                latest.getId(), id, product.getTitle(), latest.getCurrentPrice(), latest.getRawAveragePrice(),
                latest.getRoundedPrice(), latest.getFinalSuggestedPrice(), latest.getIncludedSourceCount(),
                latest.getExcludedSourceCount(), latest.getStatus().name(), latest.getOverridePrice(),
                latest.getOverrideBy(), latest.getOverrideReason(), latest.getOverrideExpiresAt(),
                latest.getApprovedBy(), latest.getApprovedAt(), latest.getCreatedAt(), List.of());

        return new ProductDetailDto(product.getId(), product.getMcOfferId(), product.getSkuOriginal(),
                product.getSkuNormalized(), product.getTitle(), product.getDescription(), product.getProductUrl(),
                product.getImageUrl(), product.getBrand(), product.getGoogleCategory(), product.getProductType(),
                product.getCondition(), product.getAvailability(), product.getCurrentWebsitePrice(),
                product.getCurrentMcPrice(), product.getCurrency(), product.isActive(), aliases, listings, recommendationDto);
    }

    public ProductDetailDto update(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id).orElseThrow(() -> NotFoundException.of("Product", id));
        if (request.title() != null) {
            product.setTitle(request.title());
        }
        product.setBrand(request.brand());
        product.setGoogleCategory(request.googleCategory());
        product.setProductType(request.productType());
        if (request.currentWebsitePrice() != null) {
            product.setCurrentWebsitePrice(request.currentWebsitePrice());
        }
        product.setActive(request.active());
        productRepository.save(product);
        auditService.record("PRODUCT_UPDATE", "PRODUCT", String.valueOf(id),
                Map.of("title", String.valueOf(request.title()), "active", request.active()));
        return getDetail(id);
    }
}
