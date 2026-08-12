package com.example.mcprice.service;

import com.example.mcprice.dto.CrawlRunDto;
import com.example.mcprice.repository.CompetitorListingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Chay doi chieu SKU (sitemap discovery) + crawl gia THAT + tinh lai recommendation cho MOT
 * doi thu, o thread rieng (khong chan request HTTP bam nut "Doi chieu SKU & tinh gia"). Phai
 * la bean RIENG voi CompetitorCrawlTriggerService — goi @Async tu cung 1 bean (self-invocation)
 * se bo qua Spring AOP proxy va chay dong bo.
 *
 * Flow: doi chieu SKU danh sach URL da kham pha (sitemap) voi san pham hien co, giong het buoc
 * "Quet lai" (SitemapDiscoveryService) — buoc nay tu crawl gia ngay cho tung listing moi/cap
 * nhat con REVIEW_REQUIRED va tu xac nhan (AUTO_CONFIRMED) neu gia hop ly (lech <= 10% so voi
 * gia hien tai), xem SitemapDiscoveryService/MatchConfirmPriceService.tryAutoConfirmByPricePlausibility
 * -> crawl gia that cho TAT CA listing da khop chac chan (AUTO_CONFIRMED/MANUALLY_CONFIRMED, ke
 * ca cac match cu tu truoc VA cac match vua duoc tu dong xac nhan o buoc tren) -> tinh lai
 * recommendation cho cac san pham lien quan. Nhung match van con REVIEW_REQUIRED sau buoc tren
 * (gia lech qua nhieu, hoac khong lay duoc gia) van phai cho nguoi dung vao trang Matching xac
 * nhan tay truoc khi duoc tinh vao gia trung binh.
 */
@Component
@RequiredArgsConstructor
public class CompetitorCrawlAsyncExecutor {

    private static final Logger log = LoggerFactory.getLogger(CompetitorCrawlAsyncExecutor.class);

    private final JobRunService jobRunService;
    private final SitemapDiscoveryService sitemapDiscoveryService;
    private final CrawlOrchestrationService crawlOrchestrationService;
    private final CompetitorListingRepository competitorListingRepository;
    private final PriceCalculationService priceCalculationService;

    @Async("discoveryExecutor")
    public void runCrawlAndRecalculate(Long competitorId, Long jobRunId) {
        jobRunService.markRunning(jobRunId);
        try {
            sitemapDiscoveryService.discoverForCompetitor(competitorId, jobRunId);
            CrawlRunDto crawlResult = crawlOrchestrationService.runCrawl(List.of(competitorId), jobRunId, "MANUAL");
            int recalculated = recalculateAffectedProducts(competitorId);
            int total = crawlResult.totalItems() + recalculated;
            int success = crawlResult.successItems() + recalculated;
            jobRunService.markFinished(jobRunId, total, success, crawlResult.failedItems(), null);
        } catch (Exception e) {
            log.warn("Doi chieu SKU + crawl + tinh lai gia ngam that bai cho competitor #{}: {}", competitorId, e.getMessage());
            jobRunService.markFailed(jobRunId, e.getMessage());
        }
    }

    private int recalculateAffectedProducts(Long competitorId) {
        List<Long> productIds = competitorListingRepository.findDistinctProductIdsByCompetitorId(competitorId);
        int count = 0;
        for (Long productId : productIds) {
            try {
                priceCalculationService.calculateForProduct(productId);
                count++;
            } catch (Exception e) {
                log.warn("Tinh lai gia loi cho product #{}: {}", productId, e.getMessage());
            }
        }
        return count;
    }
}
