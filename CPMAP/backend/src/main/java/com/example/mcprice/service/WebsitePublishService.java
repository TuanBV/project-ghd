package com.example.mcprice.service;

import com.example.mcprice.domain.RunStatus;
import com.example.mcprice.exception.BusinessRuleException;
import com.example.mcprice.config.AppProperties;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.domain.PriceRecommendation;
import com.example.mcprice.domain.RecommendationStatus;
import com.example.mcprice.repository.PriceRecommendationRepository;
import com.example.mcprice.domain.Product;
import com.example.mcprice.repository.ProductRepository;
import com.example.mcprice.adapter.MockWebsitePricePublisher;
import com.example.mcprice.adapter.WebsitePricePublisher;
import com.example.mcprice.adapter.WooCommerceWebsitePricePublisher;
import com.example.mcprice.domain.WebsitePublishItem;
import com.example.mcprice.domain.WebsitePublishRun;
import com.example.mcprice.dto.WebsitePublishRunDto;
import com.example.mcprice.repository.WebsitePublishItemRepository;
import com.example.mcprice.repository.WebsitePublishRunRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cap nhat gia website TRUOC, doc lai landing page de xac nhan, chi khi khop moi cho phep
 * MerchantSyncService dong bo tiep. Mac dinh DRY_RUN=true: khong goi adapter thuc.
 */
@Service
@RequiredArgsConstructor
public class WebsitePublishService {

    private final PriceRecommendationRepository priceRecommendationRepository;
    private final ProductRepository productRepository;
    private final CompetitorListingRepository competitorListingRepository;
    private final WebsitePublishRunRepository websitePublishRunRepository;
    private final WebsitePublishItemRepository websitePublishItemRepository;
    private final MockWebsitePricePublisher mockPublisher;
    private final WooCommerceWebsitePricePublisher wooCommercePublisher;
    private final AppProperties appProperties;
    private final AuditService auditService;

    @Transactional
    public WebsitePublishRunDto publishApprovedRecommendations(List<Long> recommendationIds, Long jobRunId) {
        boolean dryRun = appProperties.getPublish().getWebsite().isDryRun();
        List<PriceRecommendation> targets = (recommendationIds == null || recommendationIds.isEmpty())
                ? priceRecommendationRepository.findByStatus(RecommendationStatus.APPROVED,
                        org.springframework.data.domain.Pageable.unpaged()).getContent()
                : priceRecommendationRepository.findAllById(recommendationIds);

        WebsitePublishRun run = websitePublishRunRepository.save(WebsitePublishRun.builder()
                .jobRunId(jobRunId)
                .status(RunStatus.RUNNING)
                .dryRun(dryRun)
                .startedAt(OffsetDateTime.now())
                .build());

        int total = 0;
        int success = 0;
        int failed = 0;
        for (PriceRecommendation recommendation : targets) {
            if (recommendation.getStatus() != RecommendationStatus.APPROVED) {
                continue;
            }
            total++;
            WebsitePublishItem item = publishOne(run, recommendation, dryRun);
            if (item.getStatus() == WebsitePublishItem.Status.SUCCESS) {
                success++;
            } else {
                failed++;
            }
        }

        run.setTotalItems(total);
        run.setSuccessItems(success);
        run.setFailedItems(failed);
        run.setStatus(total == 0 ? RunStatus.SUCCESS : (failed == 0 ? RunStatus.SUCCESS : (success > 0 ? RunStatus.PARTIAL_SUCCESS : RunStatus.FAILED)));
        run.setFinishedAt(OffsetDateTime.now());
        WebsitePublishRun saved = websitePublishRunRepository.save(run);
        auditService.record("WEBSITE_PUBLISH_RUN", "WEBSITE_PUBLISH_RUN", String.valueOf(saved.getId()),
                Map.of("total", total, "success", success, "failed", failed, "dryRun", dryRun));
        return toDto(saved);
    }

    private WebsitePublishItem publishOne(WebsitePublishRun run, PriceRecommendation recommendation, boolean dryRun) {
        Product product = recommendation.getProduct();
        boolean hasConflict = competitorListingRepository.findByProductId(product.getId()).stream()
                .anyMatch(l -> l.getMatchStatus() == MatchStatus.REVIEW_REQUIRED && l.isActive());
        if (hasConflict) {
            return saveItem(run, product, recommendation, product.getCurrentWebsitePrice(), null,
                    WebsitePublishItem.Status.FAILED, WebsitePublishItem.VerificationStatus.NOT_CHECKED, null,
                    "Con match conflict (REVIEW_REQUIRED) chua xu ly, khong duoc publish");
        }

        BigDecimal newPrice = recommendation.effectivePrice();
        BigDecimal previousPrice = product.getCurrentWebsitePrice();
        if (newPrice == null) {
            return saveItem(run, product, recommendation, previousPrice, null, WebsitePublishItem.Status.FAILED,
                    WebsitePublishItem.VerificationStatus.NOT_CHECKED, null, "Recommendation khong co gia de xuat hop le");
        }

        if (dryRun) {
            auditService.record("WEBSITE_PUBLISH_DRY_RUN", "PRODUCT", String.valueOf(product.getId()),
                    Map.of("previousPrice", String.valueOf(previousPrice), "newPrice", newPrice.toPlainString()));
            return saveItem(run, product, recommendation, previousPrice, newPrice, WebsitePublishItem.Status.SUCCESS,
                    WebsitePublishItem.VerificationStatus.VERIFIED, newPrice, "DRY_RUN: khong goi API thuc, gia lap thanh cong");
        }

        WebsitePricePublisher publisher = resolvePublisher();
        WebsitePricePublisher.PublishResult publishResult = publisher.updateWebsitePrice(product, newPrice);
        if (!publishResult.success()) {
            return saveItem(run, product, recommendation, previousPrice, newPrice, WebsitePublishItem.Status.FAILED,
                    WebsitePublishItem.VerificationStatus.NOT_CHECKED, null, publishResult.message());
        }

        WebsitePricePublisher.VerificationResult verification = publisher.verifyLandingPagePrice(product, newPrice);
        if (!verification.matches()) {
            return saveItem(run, product, recommendation, previousPrice, newPrice, WebsitePublishItem.Status.FAILED,
                    WebsitePublishItem.VerificationStatus.MISMATCH, verification.actualPrice(), verification.message());
        }

        product.setCurrentWebsitePrice(newPrice);
        productRepository.save(product);
        auditService.record("WEBSITE_PUBLISH_SUCCESS", "PRODUCT", String.valueOf(product.getId()),
                Map.of("previousPrice", String.valueOf(previousPrice), "newPrice", newPrice.toPlainString()));
        return saveItem(run, product, recommendation, previousPrice, newPrice, WebsitePublishItem.Status.SUCCESS,
                WebsitePublishItem.VerificationStatus.VERIFIED, verification.actualPrice(), "Da cap nhat va xac nhan gia website");
    }

    private WebsitePricePublisher resolvePublisher() {
        String provider = appProperties.getPublish().getWebsite().getProvider();
        if ("WOOCOMMERCE".equalsIgnoreCase(provider)) {
            return wooCommercePublisher;
        }
        return mockPublisher;
    }

    private WebsitePublishItem saveItem(WebsitePublishRun run, Product product, PriceRecommendation recommendation,
            BigDecimal previousPrice, BigDecimal newPrice, WebsitePublishItem.Status status,
            WebsitePublishItem.VerificationStatus verificationStatus, BigDecimal verifiedPrice, String errorMessage) {
        WebsitePublishItem item = WebsitePublishItem.builder()
                .websitePublishRun(run)
                .product(product)
                .recommendation(recommendation)
                .previousPrice(previousPrice)
                .newPrice(newPrice)
                .status(status)
                .verificationStatus(verificationStatus)
                .verifiedPrice(verifiedPrice)
                .errorMessage(errorMessage)
                .build();
        return websitePublishItemRepository.save(item);
    }

    private WebsitePublishRunDto toDto(WebsitePublishRun run) {
        return new WebsitePublishRunDto(run.getId(), run.getStatus().name(), run.isDryRun(), run.getTotalItems(),
                run.getSuccessItems(), run.getFailedItems(), run.getStartedAt(), run.getFinishedAt());
    }

    public boolean isWebsiteConfirmedForProduct(Long productId, BigDecimal expectedPrice) {
        return websitePublishItemRepository.findFirstByProductIdOrderByCreatedAtDesc(productId)
                .filter(item -> item.getStatus() == WebsitePublishItem.Status.SUCCESS
                        && item.getVerificationStatus() == WebsitePublishItem.VerificationStatus.VERIFIED)
                .filter(item -> item.getNewPrice() != null && item.getNewPrice().compareTo(expectedPrice) == 0)
                .isPresent();
    }
}
