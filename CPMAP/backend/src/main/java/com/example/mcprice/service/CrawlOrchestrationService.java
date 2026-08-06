package com.example.mcprice.service;

import com.example.mcprice.domain.RunStatus;
import com.example.mcprice.domain.Competitor;
import com.example.mcprice.repository.CompetitorRepository;
import com.example.mcprice.adapter.CompetitorPriceCrawler;
import com.example.mcprice.domain.CrawlItem;
import com.example.mcprice.domain.CrawlRun;
import com.example.mcprice.dto.CrawlResult;
import com.example.mcprice.dto.CrawlRunDto;
import com.example.mcprice.repository.CrawlRunRepository;
import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.repository.CompetitorListingRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Dieu phoi crawl: chon danh sach listing theo competitor, giao cho CrawlItemExecutor chay tung item.
 * KHONG danh dau @Transactional o day: CrawlRun phai duoc commit ngay (qua transaction rieng cua
 * repository.save()) truoc khi cac item chay trong REQUIRES_NEW o CrawlItemExecutor co the tham
 * chieu toi no — neu boc chung mot transaction, cac transaction REQUIRES_NEW se khong thay duoc
 * ban ghi CrawlRun con dang uncommitted, gay loi foreign key.
 */
@Service
@RequiredArgsConstructor
public class CrawlOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(CrawlOrchestrationService.class);
    private static final List<MatchStatus> CONFIRMED_STATUSES = List.of(MatchStatus.AUTO_CONFIRMED, MatchStatus.MANUALLY_CONFIRMED);

    private final List<CompetitorPriceCrawler> crawlers;
    private final CompetitorRepository competitorRepository;
    private final CompetitorListingRepository competitorListingRepository;
    private final CrawlRunRepository crawlRunRepository;
    private final CrawlItemExecutor crawlItemExecutor;
    private final AuditService auditService;

    public CrawlRunDto runCrawl(List<Long> competitorIds, Long jobRunId, String triggerType) {
        List<Competitor> targets = (competitorIds == null || competitorIds.isEmpty())
                ? competitorRepository.findByEnabledTrue()
                : competitorRepository.findAllById(competitorIds).stream().filter(Competitor::isEnabled).toList();

        CrawlRun run = crawlRunRepository.save(CrawlRun.builder()
                .jobRunId(jobRunId)
                .triggerType(triggerType)
                .status(RunStatus.RUNNING)
                .startedAt(OffsetDateTime.now())
                .build());

        int total = 0;
        int success = 0;
        int failed = 0;
        for (Competitor competitor : targets) {
            List<CompetitorListing> listings = competitorListingRepository
                    .findByCompetitorIdAndActiveTrueAndMatchStatusIn(competitor.getId(), CONFIRMED_STATUSES);
            for (CompetitorListing listing : listings) {
                total++;
                try {
                    CrawlItem.Status itemStatus = crawlItemExecutor.crawlOne(run, competitor.getId(), listing.getId());
                    if (itemStatus == CrawlItem.Status.SUCCESS) {
                        success++;
                    } else if (itemStatus == CrawlItem.Status.FAILED) {
                        failed++;
                    }
                } catch (Exception e) {
                    log.warn("Crawl item loi ngoai du kien cho listing #{}: {}", listing.getId(), e.getMessage());
                    failed++;
                }
            }
        }

        run.setTotalItems(total);
        run.setSuccessItems(success);
        run.setFailedItems(failed);
        run.setStatus(failed == 0 ? RunStatus.SUCCESS : (success > 0 ? RunStatus.PARTIAL_SUCCESS : RunStatus.FAILED));
        run.setFinishedAt(OffsetDateTime.now());
        CrawlRun saved = crawlRunRepository.save(run);
        auditService.record("CRAWL_RUN", "CRAWL_RUN", String.valueOf(saved.getId()),
                Map.of("total", total, "success", success, "failed", failed));
        return toDto(saved);
    }

    public CrawlResult testCrawl(Long competitorId, String url) {
        Competitor competitor = competitorRepository.findById(competitorId)
                .orElseThrow(() -> com.example.mcprice.exception.NotFoundException.of("Competitor", competitorId));
        CompetitorListing transientListing = CompetitorListing.builder().competitor(competitor).url(url).build();
        CompetitorPriceCrawler crawler = crawlers.stream().filter(c -> c.supports(competitor)).findFirst().orElse(null);
        if (crawler == null) {
            return CrawlResult.failure("Khong co crawler phu hop cho crawl_mode " + competitor.getCrawlMode());
        }
        return crawler.crawl(transientListing);
    }

    private CrawlRunDto toDto(CrawlRun run) {
        return new CrawlRunDto(run.getId(), run.getStatus().name(), run.getTotalItems(), run.getSuccessItems(),
                run.getFailedItems(), run.getStartedAt(), run.getFinishedAt());
    }
}
