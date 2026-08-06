package com.example.mcprice.service;

import com.example.mcprice.exception.BusinessRuleException;
import com.example.mcprice.exception.NotFoundException;
import com.example.mcprice.util.PriceParser;
import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.domain.ObservationStatus;
import com.example.mcprice.domain.PriceObservation;
import com.example.mcprice.domain.PriceRecommendation;
import com.example.mcprice.domain.RecommendationStatus;
import com.example.mcprice.domain.SourceType;
import com.example.mcprice.dto.ManualPriceRequest;
import com.example.mcprice.dto.OverrideRequest;
import com.example.mcprice.dto.PriceRecommendationDto;
import com.example.mcprice.repository.PriceObservationRepository;
import com.example.mcprice.repository.PriceRecommendationRepository;
import com.example.mcprice.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PricingService {

    private final CompetitorListingRepository competitorListingRepository;
    private final PriceObservationRepository priceObservationRepository;
    private final PriceRecommendationRepository priceRecommendationRepository;
    private final ProductRepository productRepository;
    private final PriceCalculationService priceCalculationService;
    private final AuditService auditService;

    /** Nhap gia doi thu bang tay (khi khong crawl duoc hoac can xac minh lai). */
    public PriceObservation manualPrice(Long productId, ManualPriceRequest request) {
        CompetitorListing listing = competitorListingRepository.findById(request.competitorListingId())
                .orElseThrow(() -> NotFoundException.of("CompetitorListing", request.competitorListingId()));
        if (!listing.getProduct().getId().equals(productId)) {
            throw new BusinessRuleException("Listing #" + request.competitorListingId() + " khong thuoc san pham #" + productId);
        }
        ObservationStatus status;
        BigDecimal price = request.price();
        if (request.contactOnly() || (request.note() != null && PriceParser.isContactOnly(request.note()))) {
            status = ObservationStatus.CONTACT_ONLY;
            price = null;
        } else if (price == null || price.signum() <= 0) {
            status = ObservationStatus.NO_PRICE;
            price = null;
        } else {
            status = ObservationStatus.VALID;
        }
        PriceObservation observation = PriceObservation.builder()
                .competitorListing(listing)
                .price(price)
                .currency("VND")
                .sourceType(SourceType.MANUAL)
                .observationStatus(status)
                .rawPriceText(request.price() == null ? request.note() : request.price().toPlainString())
                .verifiedBy(currentActor())
                .note(request.note())
                .capturedAt(OffsetDateTime.now())
                .build();
        PriceObservation saved = priceObservationRepository.save(observation);
        auditService.record("MANUAL_PRICE_ENTRY", "COMPETITOR_LISTING", String.valueOf(listing.getId()),
                Map.of("productId", productId, "price", String.valueOf(price), "status", status.name()));
        return saved;
    }

    public PriceObservation excludeObservation(Long observationId, String reason) {
        PriceObservation observation = priceObservationRepository.findById(observationId)
                .orElseThrow(() -> NotFoundException.of("PriceObservation", observationId));
        observation.setExcluded(true);
        observation.setExclusionReason(reason);
        PriceObservation saved = priceObservationRepository.save(observation);
        auditService.record("OBSERVATION_EXCLUDE", "PRICE_OBSERVATION", String.valueOf(observationId), Map.of("reason", reason));
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<PriceRecommendation> findByStatus(RecommendationStatus status, Pageable pageable) {
        return priceRecommendationRepository.findByStatus(status, pageable);
    }

    public PriceRecommendationDto approve(Long recommendationId) {
        PriceRecommendation recommendation = getOrThrow(recommendationId);
        if (recommendation.getStatus() == RecommendationStatus.INSUFFICIENT_DATA) {
            throw new BusinessRuleException("Khong the approve recommendation khi INSUFFICIENT_DATA");
        }
        recommendation.setStatus(RecommendationStatus.APPROVED);
        recommendation.setApprovedBy(currentActor());
        recommendation.setApprovedAt(OffsetDateTime.now());
        PriceRecommendation saved = priceRecommendationRepository.save(recommendation);
        auditService.record("RECOMMENDATION_APPROVE", "PRICE_RECOMMENDATION", String.valueOf(recommendationId),
                Map.of("productId", recommendation.getProduct().getId(), "finalSuggestedPrice", String.valueOf(recommendation.getFinalSuggestedPrice())));
        return toSimpleDto(saved);
    }

    public PriceRecommendationDto reject(Long recommendationId, String reason) {
        PriceRecommendation recommendation = getOrThrow(recommendationId);
        recommendation.setStatus(RecommendationStatus.REJECTED);
        recommendation.setRejectedBy(currentActor());
        recommendation.setRejectedAt(OffsetDateTime.now());
        recommendation.setRejectionReason(reason);
        PriceRecommendation saved = priceRecommendationRepository.save(recommendation);
        auditService.record("RECOMMENDATION_REJECT", "PRICE_RECOMMENDATION", String.valueOf(recommendationId), Map.of("reason", String.valueOf(reason)));
        return toSimpleDto(saved);
    }

    /** Ghi de gia thu cong: bat buoc co nguoi nhap, ly do va ngay het han. */
    public PriceRecommendationDto override(Long recommendationId, OverrideRequest request) {
        PriceRecommendation recommendation = getOrThrow(recommendationId);
        recommendation.setOverridePrice(request.price());
        recommendation.setOverrideBy(currentActor());
        recommendation.setOverrideReason(request.reason());
        recommendation.setOverrideExpiresAt(request.expiresAt());
        PriceRecommendation saved = priceRecommendationRepository.save(recommendation);
        auditService.record("RECOMMENDATION_OVERRIDE", "PRICE_RECOMMENDATION", String.valueOf(recommendationId),
                Map.of("overridePrice", request.price().toPlainString(), "reason", request.reason(), "expiresAt", request.expiresAt().toString()));
        return toSimpleDto(saved);
    }

    public List<PriceRecommendationDto> recalculateAll() {
        return productRepository.findAll().stream()
                .map(p -> priceCalculationService.calculateForProduct(p.getId()))
                .toList();
    }

    /**
     * Tu dong approve cac recommendation dang READY (khong can duyet thu cong theo policy va
     * khong vuot guardrail/outlier) khi auto_publish_enabled=true. Dung trong DailyPipelineJob;
     * KHONG bao gio auto-approve recommendation REVIEW_REQUIRED/INSUFFICIENT_DATA.
     */
    public int autoApproveEligibleRecommendations() {
        List<PriceRecommendation> ready = priceRecommendationRepository
                .findByStatus(RecommendationStatus.READY, org.springframework.data.domain.Pageable.unpaged())
                .getContent();
        int approved = 0;
        for (PriceRecommendation recommendation : ready) {
            recommendation.setStatus(RecommendationStatus.APPROVED);
            recommendation.setApprovedBy("SYSTEM_AUTO_POLICY");
            recommendation.setApprovedAt(OffsetDateTime.now());
            priceRecommendationRepository.save(recommendation);
            auditService.record("RECOMMENDATION_AUTO_APPROVE", "PRICE_RECOMMENDATION",
                    String.valueOf(recommendation.getId()), Map.of("productId", recommendation.getProduct().getId()));
            approved++;
        }
        return approved;
    }

    private PriceRecommendation getOrThrow(Long id) {
        return priceRecommendationRepository.findById(id).orElseThrow(() -> NotFoundException.of("PriceRecommendation", id));
    }

    private String currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "SYSTEM" : auth.getName();
    }

    private PriceRecommendationDto toSimpleDto(PriceRecommendation r) {
        return new PriceRecommendationDto(r.getId(), r.getProduct().getId(), r.getProduct().getTitle(), r.getCurrentPrice(),
                r.getRawAveragePrice(), r.getRoundedPrice(), r.getFinalSuggestedPrice(), r.getIncludedSourceCount(),
                r.getExcludedSourceCount(), r.getStatus().name(), r.getOverridePrice(), r.getOverrideBy(), r.getOverrideReason(),
                r.getOverrideExpiresAt(), r.getApprovedBy(), r.getApprovedAt(), r.getCreatedAt(), List.of());
    }
}
