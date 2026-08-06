package com.example.mcprice.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Chay sitemap discovery cho MOT doi thu o thread rieng (khong chan request HTTP tao/kich hoat
 * doi thu). Phai la bean RIENG voi CompetitorDiscoveryTriggerService — goi @Async tu cung 1
 * bean (self-invocation) se bo qua Spring AOP proxy va chay dong bo, mat het loi ich async.
 */
@Component
@RequiredArgsConstructor
public class CompetitorDiscoveryAsyncExecutor {

    private static final Logger log = LoggerFactory.getLogger(CompetitorDiscoveryAsyncExecutor.class);

    private final JobRunService jobRunService;
    private final SitemapDiscoveryService sitemapDiscoveryService;

    @Async("discoveryExecutor")
    public void runDiscovery(Long competitorId, Long jobRunId) {
        jobRunService.markRunning(jobRunId);
        try {
            SitemapDiscoveryService.DiscoveryResult result =
                    sitemapDiscoveryService.discoverForCompetitor(competitorId, jobRunId);
            jobRunService.markFinished(jobRunId, result.totalUrlsScanned(), result.totalUrlsScanned(), 0, null);
        } catch (Exception e) {
            log.warn("Discovery ngam that bai cho competitor #{}: {}", competitorId, e.getMessage());
            jobRunService.markFailed(jobRunId, e.getMessage());
        }
    }
}
