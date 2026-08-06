package com.example.mcprice.service;

import com.example.mcprice.exception.NotFoundException;
import com.example.mcprice.util.MoneyUtil;
import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.domain.ObservationStatus;
import com.example.mcprice.domain.OutlierStrategy;
import com.example.mcprice.domain.PolicyScope;
import com.example.mcprice.domain.PriceObservation;
import com.example.mcprice.domain.PricePolicy;
import com.example.mcprice.domain.PriceRecommendation;
import com.example.mcprice.domain.PriceRecommendationSource;
import com.example.mcprice.domain.RecommendationStatus;
import com.example.mcprice.dto.PriceRecommendationDto;
import com.example.mcprice.repository.PriceObservationRepository;
import com.example.mcprice.repository.PricePolicyRepository;
import com.example.mcprice.repository.PriceRecommendationRepository;
import com.example.mcprice.repository.PriceRecommendationSourceRepository;
import com.example.mcprice.domain.Product;
import com.example.mcprice.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dong co tinh gia de xuat: TRUNG BINH CONG gia hop le cua cac website doi thu (khong cong
 * margin, khong dung Gia Min). Tuan thu dung trinh tu muc 8 cua dac ta nghiep vu.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PriceCalculationService {

    private static final List<MatchStatus> CONFIRMED_STATUSES = List.of(MatchStatus.AUTO_CONFIRMED, MatchStatus.MANUALLY_CONFIRMED);

    private final ProductRepository productRepository;
    private final CompetitorListingRepository competitorListingRepository;
    private final PriceObservationRepository priceObservationRepository;
    private final PricePolicyRepository pricePolicyRepository;
    private final PriceRecommendationRepository priceRecommendationRepository;
    private final PriceRecommendationSourceRepository priceRecommendationSourceRepository;
    private final AuditService auditService;

    private record SourceCandidate(PriceObservation observation, CompetitorListing listing, boolean included, String reason) {
    }

    public PriceRecommendationDto calculateForProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> NotFoundException.of("Product", productId));
        PricePolicy policy = resolveEffectivePolicy(product);

        List<CompetitorListing> confirmedListings = competitorListingRepository
                .findConfirmedListingsForProduct(productId, CONFIRMED_STATUSES);

        OffsetDateTime staleThreshold = OffsetDateTime.now().minusHours(policy.getMaxObservationAgeHours());
        List<SourceCandidate> candidates = new ArrayList<>();

        for (CompetitorListing listing : confirmedListings) {
            if (!listing.isActive() || !listing.getCompetitor().isEnabled()) {
                continue;
            }
            PriceObservation latest = priceObservationRepository
                    .findFirstByCompetitorListingIdOrderByCapturedAtDesc(listing.getId()).orElse(null);
            if (latest == null) {
                continue;
            }
            candidates.add(evaluateCandidate(latest, listing, product, staleThreshold));
        }

        List<SourceCandidate> included = candidates.stream().filter(SourceCandidate::included).toList();
        List<BigDecimal> validPrices = included.stream().map(c -> c.observation().getPrice()).toList();

        PriceRecommendation recommendation;
        if (validPrices.size() < policy.getMinimumCompetitorCount()) {
            recommendation = buildInsufficientData(product, policy, candidates, validPrices.size());
        } else {
            recommendation = buildCalculatedRecommendation(product, policy, candidates, included, validPrices);
        }

        PriceRecommendation saved = priceRecommendationRepository.save(recommendation);
        persistSources(saved, candidates);
        auditService.record("PRICE_CALCULATE", "PRODUCT", String.valueOf(productId),
                Map.of("recommendationId", saved.getId(), "status", saved.getStatus().name(),
                        "includedSourceCount", saved.getIncludedSourceCount()));
        return toDto(saved, candidates);
    }

    private SourceCandidate evaluateCandidate(PriceObservation observation, CompetitorListing listing, Product product,
                                               OffsetDateTime staleThreshold) {
        if (observation.isExcluded()) {
            return new SourceCandidate(observation, listing, false, "Da bi loai thu cong: " + observation.getExclusionReason());
        }
        if (observation.getObservationStatus() == ObservationStatus.CONTACT_ONLY) {
            return new SourceCandidate(observation, listing, false, "Gia dang 'Lien he', khong co gia so");
        }
        if (observation.getObservationStatus() == ObservationStatus.PARSE_ERROR) {
            return new SourceCandidate(observation, listing, false, "Loi parse gia tu nguon");
        }
        if (observation.getObservationStatus() == ObservationStatus.NO_PRICE) {
            return new SourceCandidate(observation, listing, false, "Khong co gia (chua xac minh / khong tim thay)");
        }
        if (observation.getObservationStatus() == ObservationStatus.OUT_OF_STOCK) {
            return new SourceCandidate(observation, listing, false, "San pham dang het hang o doi thu");
        }
        if (!"VND".equalsIgnoreCase(observation.getCurrency())) {
            return new SourceCandidate(observation, listing, false, "Sai loai tien te: " + observation.getCurrency());
        }
        if (observation.getPrice() == null || observation.getPrice().signum() <= 0) {
            return new SourceCandidate(observation, listing, false, "Gia khong hop le (<= 0 hoac null)");
        }
        if (observation.getObservationStatus() == ObservationStatus.STALE || observation.getCapturedAt().isBefore(staleThreshold)) {
            return new SourceCandidate(observation, listing, false, "Du lieu qua cu (stale), vuot nguong " + staleThreshold);
        }
        return new SourceCandidate(observation, listing, true, "Hop le");
    }

    private PriceRecommendation buildInsufficientData(Product product, PricePolicy policy, List<SourceCandidate> candidates,
                                                        int validCount) {
        Map<String, Object> snapshot = baseSnapshot(policy, candidates);
        snapshot.put("reason", "So nguon hop le (" + validCount + ") nho hon minimumCompetitorCount ("
                + policy.getMinimumCompetitorCount() + ")");
        return PriceRecommendation.builder()
                .product(product)
                .currentPrice(product.getCurrentWebsitePrice())
                .rawAveragePrice(null)
                .roundedPrice(null)
                .finalSuggestedPrice(product.getCurrentWebsitePrice())
                .includedSourceCount(validCount)
                .excludedSourceCount(candidates.size() - validCount)
                .calculationSnapshot(snapshot)
                .status(RecommendationStatus.INSUFFICIENT_DATA)
                .build();
    }

    private PriceRecommendation buildCalculatedRecommendation(Product product, PricePolicy policy,
            List<SourceCandidate> candidates, List<SourceCandidate> included, List<BigDecimal> validPrices) {
        BigDecimal rawAverage = MoneyUtil.average(validPrices);
        boolean outlierFlagged = false;
        List<String> outlierDetails = new ArrayList<>();
        for (SourceCandidate c : included) {
            BigDecimal deviation = MoneyUtil.percentChange(rawAverage, c.observation().getPrice()).abs();
            if (deviation.compareTo(policy.getOutlierThresholdPercent()) > 0) {
                outlierFlagged = true;
                outlierDetails.add("Observation #" + c.observation().getId() + " lech " + deviation + "% so voi trung binh");
            }
        }

        List<SourceCandidate> effectiveIncluded = included;
        BigDecimal effectiveAverage = rawAverage;
        if (outlierFlagged && policy.getOutlierStrategy() == OutlierStrategy.EXCLUDE) {
            BigDecimal averageForOutlierCheck = rawAverage;
            effectiveIncluded = included.stream()
                    .filter(c -> MoneyUtil.percentChange(averageForOutlierCheck, c.observation().getPrice()).abs()
                            .compareTo(policy.getOutlierThresholdPercent()) <= 0)
                    .toList();
            List<BigDecimal> effectivePrices = effectiveIncluded.stream().map(c -> c.observation().getPrice()).toList();
            effectiveAverage = MoneyUtil.average(effectivePrices);
        }
        rawAverage = effectiveAverage;

        BigDecimal roundedPrice = MoneyUtil.roundToStep(rawAverage, policy.getRoundingStep());
        BigDecimal currentPrice = product.getCurrentWebsitePrice();
        BigDecimal percentChange = MoneyUtil.percentChange(currentPrice, roundedPrice);

        boolean guardrailBreached = percentChange != null && (
                percentChange.compareTo(policy.getMaxIncreasePercent()) > 0
                        || percentChange.compareTo(policy.getMaxDecreasePercent().negate()) < 0);

        boolean outOfAllowedRange = (policy.getMinimumAllowedPrice() != null && roundedPrice.compareTo(policy.getMinimumAllowedPrice()) < 0)
                || (policy.getMaximumAllowedPrice() != null && roundedPrice.compareTo(policy.getMaximumAllowedPrice()) > 0);

        RecommendationStatus status;
        if (policy.isRequireManualApproval() || guardrailBreached || outlierFlagged || outOfAllowedRange) {
            status = RecommendationStatus.REVIEW_REQUIRED;
        } else {
            status = RecommendationStatus.READY;
        }

        Map<String, Object> snapshot = baseSnapshot(policy, candidates);
        snapshot.put("rawAveragePrice", rawAverage.toPlainString());
        snapshot.put("roundedPrice", roundedPrice.toPlainString());
        snapshot.put("percentChange", percentChange == null ? null : percentChange.toPlainString());
        snapshot.put("outlierFlagged", outlierFlagged);
        snapshot.put("outlierDetails", outlierDetails);
        snapshot.put("guardrailBreached", guardrailBreached);
        snapshot.put("outOfAllowedRange", outOfAllowedRange);

        return PriceRecommendation.builder()
                .product(product)
                .currentPrice(currentPrice)
                .rawAveragePrice(rawAverage)
                .roundedPrice(roundedPrice)
                .finalSuggestedPrice(roundedPrice)
                .includedSourceCount(effectiveIncluded.size())
                .excludedSourceCount(candidates.size() - effectiveIncluded.size())
                .calculationSnapshot(snapshot)
                .status(status)
                .build();
    }

    private Map<String, Object> baseSnapshot(PricePolicy policy, List<SourceCandidate> candidates) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("calculatedAt", OffsetDateTime.now().toString());
        snapshot.put("minimumCompetitorCount", policy.getMinimumCompetitorCount());
        snapshot.put("maxObservationAgeHours", policy.getMaxObservationAgeHours());
        snapshot.put("roundingStep", policy.getRoundingStep().toPlainString());
        snapshot.put("maxIncreasePercent", policy.getMaxIncreasePercent().toPlainString());
        snapshot.put("maxDecreasePercent", policy.getMaxDecreasePercent().toPlainString());
        snapshot.put("outlierThresholdPercent", policy.getOutlierThresholdPercent().toPlainString());
        snapshot.put("outlierStrategy", policy.getOutlierStrategy().name());
        List<Map<String, Object>> sourceEntries = candidates.stream().map(c -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("competitorId", c.listing().getCompetitor().getId());
            entry.put("competitorName", c.listing().getCompetitor().getName());
            entry.put("observationId", c.observation().getId());
            entry.put("price", c.observation().getPrice() == null ? null : c.observation().getPrice().toPlainString());
            entry.put("capturedAt", c.observation().getCapturedAt().toString());
            entry.put("included", c.included());
            entry.put("reason", c.reason());
            return entry;
        }).toList();
        snapshot.put("sources", sourceEntries);
        return snapshot;
    }

    private void persistSources(PriceRecommendation recommendation, List<SourceCandidate> candidates) {
        for (SourceCandidate c : candidates) {
            priceRecommendationSourceRepository.save(PriceRecommendationSource.builder()
                    .recommendation(recommendation)
                    .priceObservation(c.observation())
                    .included(c.included())
                    .exclusionReason(c.included() ? null : c.reason())
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public PricePolicy resolveEffectivePolicy(Product product) {
        return pricePolicyRepository.findByScopeAndProductId(PolicyScope.PRODUCT, product.getId())
                .or(() -> product.getGoogleCategory() == null ? java.util.Optional.empty()
                        : pricePolicyRepository.findByScopeAndCategory(PolicyScope.CATEGORY, product.getGoogleCategory()))
                .or(() -> pricePolicyRepository.findByScope(PolicyScope.GLOBAL))
                .orElseGet(() -> PricePolicy.builder().scope(PolicyScope.GLOBAL).build());
    }

    private PriceRecommendationDto toDto(PriceRecommendation r, List<SourceCandidate> candidates) {
        List<PriceRecommendationDto.SourceBreakdown> sources = candidates.stream()
                .map(c -> new PriceRecommendationDto.SourceBreakdown(
                        c.observation().getId(), c.listing().getCompetitor().getId(), c.listing().getCompetitor().getName(),
                        c.observation().getPrice(), c.observation().getCapturedAt(), c.included(), c.reason()))
                .toList();
        return new PriceRecommendationDto(r.getId(), r.getProduct().getId(), r.getProduct().getTitle(), r.getCurrentPrice(),
                r.getRawAveragePrice(), r.getRoundedPrice(), r.getFinalSuggestedPrice(), r.getIncludedSourceCount(),
                r.getExcludedSourceCount(), r.getStatus().name(), r.getOverridePrice(), r.getOverrideBy(), r.getOverrideReason(),
                r.getOverrideExpiresAt(), r.getApprovedBy(), r.getApprovedAt(), r.getCreatedAt(), sources);
    }
}
