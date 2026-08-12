package com.example.mcprice.service;

import com.example.mcprice.adapter.OwnWebsitePriceFetcher;
import com.example.mcprice.exception.NotFoundException;
import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.domain.PriceObservation;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.domain.PriceRecommendation;
import com.example.mcprice.repository.PriceObservationRepository;
import com.example.mcprice.repository.PriceRecommendationRepository;
import com.example.mcprice.domain.Product;
import com.example.mcprice.dto.AliasDto;
import com.example.mcprice.dto.ProductDetailDto;
import com.example.mcprice.dto.ProductSummaryDto;
import com.example.mcprice.dto.ProductUpdateRequest;
import com.example.mcprice.repository.ProductAliasRepository;
import com.example.mcprice.repository.ProductRepository;
import com.example.mcprice.repository.ProductSpecifications;
import com.example.mcprice.util.ProductUrlBuilder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private static final List<MatchStatus> CONFIRMED_STATUSES = List.of(MatchStatus.AUTO_CONFIRMED, MatchStatus.MANUALLY_CONFIRMED);
    private static final BigDecimal ROUNDING_UNIT = BigDecimal.valueOf(10_000);

    private final ProductRepository productRepository;
    private final ProductAliasRepository productAliasRepository;
    private final CompetitorListingRepository competitorListingRepository;
    private final PriceRecommendationRepository priceRecommendationRepository;
    private final PriceObservationRepository priceObservationRepository;
    private final MatchingService matchingService;
    private final AuditService auditService;
    private final OwnWebsitePriceFetcher ownWebsitePriceFetcher;

    @Transactional(readOnly = true)
    public Page<ProductSummaryDto> search(String keyword, String category, String availability, Long competitorId, Pageable pageable) {
        Specification<Product> competitorSpec = competitorId == null ? null
                : ProductSpecifications.idIn(competitorListingRepository.findDistinctProductIdsByCompetitorId(competitorId));
        var spec = ProductSpecifications.allOf(
                ProductSpecifications.search(keyword),
                ProductSpecifications.category(category),
                ProductSpecifications.availability(availability),
                competitorSpec,
                ProductSpecifications.orderByReviewNeededThenInStock());
        Page<Product> page = productRepository.findAll(spec, pageable);
        List<Long> productIds = page.getContent().stream().map(Product::getId).toList();

        List<PriceRecommendation> latestRecommendations = productIds.isEmpty()
                ? List.of() : priceRecommendationRepository.findLatestForProducts(productIds);
        Map<Long, PriceRecommendation> recommendationByProduct = latestRecommendations.stream()
                .collect(java.util.stream.Collectors.toMap(r -> r.getProduct().getId(), r -> r, (a, b) -> a));

        var listingsForPage = productIds.isEmpty() ? List.<CompetitorListing>of()
                : competitorListingRepository.findByProductIdIn(productIds);

        var confirmedListings = listingsForPage.stream()
                .filter(l -> l.isActive() && CONFIRMED_STATUSES.contains(l.getMatchStatus()))
                .toList();

        Map<Long, Long> confirmedCountByProduct = confirmedListings.stream()
                .collect(Collectors.groupingBy(l -> l.getProduct().getId(), Collectors.counting()));

        List<Long> confirmedListingIds = confirmedListings.stream().map(CompetitorListing::getId).toList();
        List<PriceObservation> latestObservations = confirmedListingIds.isEmpty()
                ? List.of() : priceObservationRepository.findLatestForListings(confirmedListingIds);
        Map<Long, BigDecimal> priceByListingId = latestObservations.stream()
                .filter(o -> o.getPrice() != null)
                .collect(Collectors.toMap(o -> o.getCompetitorListing().getId(), PriceObservation::getPrice, (a, b) -> a));

        Map<Long, BigDecimal> averagePriceByProduct = confirmedListings.stream()
                .collect(Collectors.groupingBy(l -> l.getProduct().getId(),
                        Collectors.collectingAndThen(Collectors.toList(), listings -> averageOf(listings, priceByListingId))));

        Map<Long, Boolean> conflictByProduct = listingsForPage.stream()
                .collect(Collectors.groupingBy(l -> l.getProduct().getId(),
                        Collectors.mapping(l -> l.getMatchStatus() == MatchStatus.REVIEW_REQUIRED, Collectors.reducing(false, Boolean::logicalOr))));

        return page.map(p -> {
            PriceRecommendation rec = recommendationByProduct.get(p.getId());
            return new ProductSummaryDto(
                    p.getId(), p.getSkuOriginal(), p.getTitle(), p.getProductUrl(), p.getBrand(), p.getGoogleCategory(),
                    p.getAvailability(), p.getCurrentWebsitePrice(), p.getCurrentMcPrice(), p.getCurrency(),
                    confirmedCountByProduct.getOrDefault(p.getId(), 0L),
                    averagePriceByProduct.get(p.getId()),
                    rec == null ? null : rec.getFinalSuggestedPrice(),
                    rec == null ? null : rec.getStatus().name(),
                    conflictByProduct.getOrDefault(p.getId(), false));
        });
    }

    private static BigDecimal averageOf(List<CompetitorListing> listings, Map<Long, BigDecimal> priceByListingId) {
        List<BigDecimal> prices = listings.stream()
                .map(l -> priceByListingId.get(l.getId()))
                .filter(Objects::nonNull)
                .toList();
        if (prices.isEmpty()) {
            return null;
        }
        BigDecimal sum = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rawAverage = sum.divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP);
        return rawAverage.divide(ROUNDING_UNIT, 0, RoundingMode.HALF_UP).multiply(ROUNDING_UNIT);
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

        BigDecimal resolvedPrice = request.currentWebsitePrice();
        String newUrl = request.productUrl() == null ? null : request.productUrl().trim();
        if (newUrl != null && !newUrl.isEmpty() && !newUrl.equals(product.getProductUrl())) {
            // URL vua duoc nhap/sua khac voi URL cu — tu dong lay gia tren trang do lam gia mac
            // dinh, nguoi dung van sua tay lai duoc sau. Loi crawl (mang, khong tim thay gia...)
            // khong duoc chan viec luu URL/cac thong tin khac trong cung request nay.
            product.setProductUrl(newUrl);
            BigDecimal fetchedPrice = ownWebsitePriceFetcher.fetchPrice(newUrl);
            if (fetchedPrice != null) {
                resolvedPrice = fetchedPrice;
            }
        }
        if (resolvedPrice != null) {
            product.setCurrentWebsitePrice(resolvedPrice);
        }
        if (request.availability() != null && !request.availability().isBlank()) {
            product.setAvailability(request.availability());
        }
        product.setActive(request.active());
        productRepository.save(product);
        auditService.record("PRODUCT_UPDATE", "PRODUCT", String.valueOf(id),
                Map.of("title", String.valueOf(request.title()), "active", request.active()));
        return getDetail(id);
    }

    /**
     * Sinh lai productUrl (tren website cua chinh minh) cho cac san pham dang thieu URL, dua vao
     * ten san pham — dung cho san pham import qua CSV_PRODUCT (khong co cot URL) truoc khi tinh
     * nang nay ton tai. Chi dien vao cho ban ghi dang null, khong ghi de URL da co san (vd tu MC feed).
     */
    public int backfillMissingProductUrls() {
        List<Product> missingUrl = productRepository.findByProductUrlIsNull();
        int updated = 0;
        for (Product product : missingUrl) {
            String url = ProductUrlBuilder.buildOwnWebsiteUrl(product.getTitle());
            if (url != null) {
                product.setProductUrl(url);
                updated++;
            }
        }
        productRepository.saveAll(missingUrl);
        auditService.record("PRODUCT_BACKFILL_URLS", "PRODUCT", "bulk",
                Map.of("candidateCount", missingUrl.size(), "updatedCount", updated));
        return updated;
    }
}
