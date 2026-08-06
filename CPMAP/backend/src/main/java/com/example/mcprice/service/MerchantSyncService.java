package com.example.mcprice.service;

import com.example.mcprice.domain.RunStatus;
import com.example.mcprice.config.AppProperties;
import com.example.mcprice.adapter.GoogleMerchantApiPublisher;
import com.example.mcprice.adapter.MerchantCenterPublisher;
import com.example.mcprice.adapter.MockMerchantCenterPublisher;
import com.example.mcprice.domain.MerchantSyncItem;
import com.example.mcprice.domain.MerchantSyncRun;
import com.example.mcprice.dto.MerchantSyncRunDto;
import com.example.mcprice.repository.MerchantSyncItemRepository;
import com.example.mcprice.repository.MerchantSyncRunRepository;
import com.example.mcprice.domain.PriceRecommendation;
import com.example.mcprice.domain.RecommendationStatus;
import com.example.mcprice.repository.PriceRecommendationRepository;
import com.example.mcprice.domain.Product;
import com.example.mcprice.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dong bo gia len Google Merchant Center — CHI thuc hien sau khi website da cap nhat va
 * xac nhan dung gia (WebsitePublishService.isWebsiteConfirmedForProduct). Mac dinh DRY_RUN=true.
 */
@Service
@RequiredArgsConstructor
public class MerchantSyncService {

    private final PriceRecommendationRepository priceRecommendationRepository;
    private final ProductRepository productRepository;
    private final MerchantSyncRunRepository merchantSyncRunRepository;
    private final MerchantSyncItemRepository merchantSyncItemRepository;
    private final WebsitePublishService websitePublishService;
    private final MockMerchantCenterPublisher mockPublisher;
    private final GoogleMerchantApiPublisher googlePublisher;
    private final AppProperties appProperties;
    private final AuditService auditService;

    @Transactional
    public MerchantSyncRunDto syncApprovedRecommendations(List<Long> recommendationIds, Long jobRunId) {
        boolean dryRun = appProperties.getPublish().getMerchant().isDryRun();
        List<PriceRecommendation> targets = (recommendationIds == null || recommendationIds.isEmpty())
                ? priceRecommendationRepository.findByStatus(RecommendationStatus.APPROVED,
                        org.springframework.data.domain.Pageable.unpaged()).getContent()
                : priceRecommendationRepository.findAllById(recommendationIds);

        MerchantSyncRun run = merchantSyncRunRepository.save(MerchantSyncRun.builder()
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
            MerchantSyncItem item = syncOne(run, recommendation, dryRun);
            if (item.getStatus() == MerchantSyncItem.Status.SUCCESS) {
                success++;
            } else if (item.getStatus() == MerchantSyncItem.Status.FAILED) {
                failed++;
            }
        }

        run.setTotalItems(total);
        run.setSuccessItems(success);
        run.setFailedItems(failed);
        run.setStatus(total == 0 ? RunStatus.SUCCESS : (failed == 0 ? RunStatus.SUCCESS : (success > 0 ? RunStatus.PARTIAL_SUCCESS : RunStatus.FAILED)));
        run.setFinishedAt(OffsetDateTime.now());
        MerchantSyncRun saved = merchantSyncRunRepository.save(run);
        auditService.record("MERCHANT_SYNC_RUN", "MERCHANT_SYNC_RUN", String.valueOf(saved.getId()),
                Map.of("total", total, "success", success, "failed", failed, "dryRun", dryRun));
        return toDto(saved);
    }

    private MerchantSyncItem syncOne(MerchantSyncRun run, PriceRecommendation recommendation, boolean dryRun) {
        Product product = recommendation.getProduct();
        BigDecimal newPrice = recommendation.effectivePrice();
        BigDecimal previousPrice = product.getCurrentMcPrice();

        if (newPrice == null || !websitePublishService.isWebsiteConfirmedForProduct(product.getId(), newPrice)) {
            return saveItem(run, product, recommendation, previousPrice, newPrice, MerchantSyncItem.Status.SKIPPED,
                    "NOT_ELIGIBLE", "Website chua cap nhat/xac nhan dung gia nay, tuyet doi khong day len Merchant Center");
        }

        if (dryRun) {
            auditService.record("MERCHANT_SYNC_DRY_RUN", "PRODUCT", String.valueOf(product.getId()),
                    Map.of("previousPrice", String.valueOf(previousPrice), "newPrice", newPrice.toPlainString()));
            return saveItem(run, product, recommendation, previousPrice, newPrice, MerchantSyncItem.Status.SUCCESS,
                    "DRY_RUN", "DRY_RUN: khong goi Google Merchant API thuc, gia lap thanh cong");
        }

        MerchantCenterPublisher publisher = resolvePublisher();
        MerchantCenterPublisher.PublishResult publishResult = publisher.updateMerchantPrice(product, newPrice);
        if (!publishResult.success()) {
            return saveItem(run, product, recommendation, previousPrice, newPrice, MerchantSyncItem.Status.FAILED,
                    "FAILED", publishResult.message());
        }

        MerchantCenterPublisher.MerchantStatusResult status = publisher.checkMerchantStatus(product);
        product.setCurrentMcPrice(newPrice);
        productRepository.save(product);
        recommendation.setStatus(RecommendationStatus.PUBLISHED);
        priceRecommendationRepository.save(recommendation);
        auditService.record("MERCHANT_SYNC_SUCCESS", "PRODUCT", String.valueOf(product.getId()),
                Map.of("previousPrice", String.valueOf(previousPrice), "newPrice", newPrice.toPlainString(), "merchantStatus", status.status()));
        return saveItem(run, product, recommendation, previousPrice, newPrice, MerchantSyncItem.Status.SUCCESS,
                status.status(), status.message());
    }

    private MerchantCenterPublisher resolvePublisher() {
        String provider = appProperties.getPublish().getMerchant().getProvider();
        if ("GOOGLE_API".equalsIgnoreCase(provider)) {
            return googlePublisher;
        }
        return mockPublisher;
    }

    private MerchantSyncItem saveItem(MerchantSyncRun run, Product product, PriceRecommendation recommendation,
            BigDecimal previousPrice, BigDecimal newPrice, MerchantSyncItem.Status status, String merchantStatus,
            String message) {
        return merchantSyncItemRepository.save(MerchantSyncItem.builder()
                .merchantSyncRun(run)
                .product(product)
                .recommendation(recommendation)
                .previousPrice(previousPrice)
                .newPrice(newPrice)
                .status(status)
                .merchantStatus(merchantStatus)
                .errorMessage(status == MerchantSyncItem.Status.SUCCESS ? null : message)
                .build());
    }

    private MerchantSyncRunDto toDto(MerchantSyncRun run) {
        return new MerchantSyncRunDto(run.getId(), run.getStatus().name(), run.isDryRun(), run.getTotalItems(),
                run.getSuccessItems(), run.getFailedItems(), run.getStartedAt(), run.getFinishedAt());
    }
}
