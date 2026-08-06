package com.example.mcprice.service;

import com.example.mcprice.domain.Competitor;
import com.example.mcprice.repository.CompetitorRepository;
import com.example.mcprice.adapter.CompetitorPriceCrawler;
import com.example.mcprice.adapter.DomainRateLimiter;
import com.example.mcprice.domain.CrawlItem;
import com.example.mcprice.domain.CrawlRun;
import com.example.mcprice.dto.CrawlResult;
import com.example.mcprice.repository.CrawlItemRepository;
import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.domain.PriceObservation;
import com.example.mcprice.domain.SourceType;
import com.example.mcprice.repository.PriceObservationRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thuc thi crawl cho MOT listing trong transaction rieng (REQUIRES_NEW) de mot item loi
 * khong lam rollback ca crawl run. Load lai Competitor/CompetitorListing bang ID ngay trong
 * transaction nay — KHONG dung lai entity duoc truyen tu vong lap ben ngoai, vi entity do da
 * detached va co the mang version cu (gay OptimisticLockException khi cung mot competitor
 * duoc cap nhat qua nhieu lan goi REQUIRES_NEW lien tiep).
 */
@Service
@RequiredArgsConstructor
public class CrawlItemExecutor {

    private final List<CompetitorPriceCrawler> crawlers;
    private final CompetitorRepository competitorRepository;
    private final CompetitorListingRepository competitorListingRepository;
    private final PriceObservationRepository priceObservationRepository;
    private final CrawlItemRepository crawlItemRepository;
    private final DomainRateLimiter domainRateLimiter;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CrawlItem.Status crawlOne(CrawlRun run, Long competitorId, Long listingId) {
        Competitor competitor = competitorRepository.findById(competitorId).orElseThrow();
        CompetitorListing listing = competitorListingRepository.findById(listingId).orElseThrow();

        domainRateLimiter.acquire(competitor.getId(), competitor.getRequestsPerMinute());
        CompetitorPriceCrawler crawler = crawlers.stream().filter(c -> c.supports(competitor)).findFirst().orElse(null);
        if (crawler == null) {
            saveItem(run, listing, CrawlItem.Status.SKIPPED, null,
                    "Khong co crawler phu hop cho crawl_mode " + competitor.getCrawlMode());
            return CrawlItem.Status.SKIPPED;
        }
        CrawlResult result = crawler.crawl(listing);
        if (!result.success()) {
            competitor.setLastErrorAt(OffsetDateTime.now());
            competitor.setLastErrorMessage(result.errorMessage());
            competitorRepository.save(competitor);
            saveItem(run, listing, CrawlItem.Status.FAILED, null, result.errorMessage());
            return CrawlItem.Status.FAILED;
        }
        PriceObservation observation = priceObservationRepository.save(PriceObservation.builder()
                .competitorListing(listing)
                .price(result.price())
                .currency("VND")
                .sourceType(SourceType.CRAWL)
                .observationStatus(result.observationStatus())
                .rawPriceText(result.rawPriceText())
                .availability(result.availability())
                .httpStatus(result.httpStatus())
                .finalUrl(result.finalUrl())
                .capturedAt(OffsetDateTime.now())
                .build());
        competitor.setLastSuccessAt(OffsetDateTime.now());
        competitorRepository.save(competitor);
        saveItem(run, listing, CrawlItem.Status.SUCCESS, observation, null);
        return CrawlItem.Status.SUCCESS;
    }

    private void saveItem(CrawlRun run, CompetitorListing listing, CrawlItem.Status status, PriceObservation observation,
                           String error) {
        crawlItemRepository.save(CrawlItem.builder()
                .crawlRun(run)
                .competitorListing(listing)
                .status(status)
                .priceObservation(observation)
                .errorMessage(error)
                .build());
    }
}
