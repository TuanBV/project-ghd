package com.example.mcprice.job;

import com.example.mcprice.repository.CompetitorRepository;
import com.example.mcprice.service.SitemapDiscoveryService;
import com.example.mcprice.domain.JobKeys;
import com.example.mcprice.service.JobRunService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Kham pha URL san pham qua sitemap.xml cho moi doi thu da cau hinh sitemapUrl (hop le theo
 * robots.txt). Doi thu chua cau hinh (vi du dienmayabc.com — bi chan boi robots.txt) se tu
 * dong bo qua, khong crawl.
 */
public class CompetitorDiscoveryJob extends QuartzJobBean {

    private static final Logger log = LoggerFactory.getLogger(CompetitorDiscoveryJob.class);

    @Autowired
    private CompetitorRepository competitorRepository;
    @Autowired
    private SitemapDiscoveryService sitemapDiscoveryService;
    @Autowired
    private JobRunService jobRunService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobExecutionHelper.run(context, jobRunService, JobKeys.COMPETITOR_DISCOVERY, jobRunId -> {
            var competitors = competitorRepository.findByEnabledTrue();
            int total = 0;
            int success = 0;
            int failed = 0;
            for (var competitor : competitors) {
                if (!competitor.getExtractorConfig().containsKey("sitemapUrl")) {
                    continue;
                }
                total++;
                try {
                    var result = sitemapDiscoveryService.discoverForCompetitor(competitor.getId());
                    log.info("[CompetitorDiscovery] {}: scanned={} autoConfirmed={} reviewRequired={} conflicts={} noMatch={}",
                            competitor.getName(), result.totalUrlsScanned(), result.autoConfirmed(),
                            result.reviewRequired(), result.conflicts(), result.noMatch());
                    success++;
                } catch (Exception e) {
                    log.error("[CompetitorDiscovery] Loi voi {}: {}", competitor.getName(), e.getMessage(), e);
                    failed++;
                }
            }
            return new int[] { total, success, failed };
        });
    }
}
