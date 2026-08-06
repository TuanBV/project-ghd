package com.example.mcprice.job;

import com.example.mcprice.domain.JobKeys;
import com.example.mcprice.service.JobRunService;
import com.example.mcprice.service.MerchantSyncService;
import com.example.mcprice.service.PricePolicyService;
import com.example.mcprice.service.PricingService;
import com.example.mcprice.service.StaleObservationCleanupService;
import com.example.mcprice.service.CrawlOrchestrationService;
import com.example.mcprice.service.WebsitePublishService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Job tong hang ngay: crawl -> validate (stale cleanup) -> calculate -> approve/auto-policy
 * -> update website -> verify -> sync MC. Chay luc 02:00 Asia/Ho_Chi_Minh theo cron trong Settings.
 */
public class DailyPipelineJob extends QuartzJobBean {

    private static final Logger log = LoggerFactory.getLogger(DailyPipelineJob.class);

    @Autowired
    private CrawlOrchestrationService crawlOrchestrationService;
    @Autowired
    private StaleObservationCleanupService staleObservationCleanupService;
    @Autowired
    private PricingService pricingService;
    @Autowired
    private PricePolicyService pricePolicyService;
    @Autowired
    private WebsitePublishService websitePublishService;
    @Autowired
    private MerchantSyncService merchantSyncService;
    @Autowired
    private JobRunService jobRunService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobExecutionHelper.run(context, jobRunService, JobKeys.DAILY_PIPELINE, jobRunId -> {
            int totalSteps = 6;
            int okSteps = 0;

            var crawlResult = crawlOrchestrationService.runCrawl(null, jobRunId, "SCHEDULED");
            log.info("[DailyPipeline] crawl: total={} success={} failed={}", crawlResult.totalItems(),
                    crawlResult.successItems(), crawlResult.failedItems());
            okSteps++;

            int staleCount = staleObservationCleanupService.markStaleObservations();
            log.info("[DailyPipeline] stale cleanup: {} observation(s) danh dau STALE", staleCount);
            okSteps++;

            pricingService.recalculateAll();
            log.info("[DailyPipeline] price calculation done");
            okSteps++;

            boolean autoPublishEnabled = pricePolicyService.getGlobalPolicy().autoPublishEnabled();
            int approved = autoPublishEnabled ? pricingService.autoApproveEligibleRecommendations() : 0;
            log.info("[DailyPipeline] auto-approve: enabled={} approvedCount={}", autoPublishEnabled, approved);
            okSteps++;

            var websiteRun = websitePublishService.publishApprovedRecommendations(null, jobRunId);
            log.info("[DailyPipeline] website publish: total={} success={} failed={}", websiteRun.totalItems(),
                    websiteRun.successItems(), websiteRun.failedItems());
            okSteps++;

            var merchantRun = merchantSyncService.syncApprovedRecommendations(null, jobRunId);
            log.info("[DailyPipeline] merchant sync: total={} success={} failed={}", merchantRun.totalItems(),
                    merchantRun.successItems(), merchantRun.failedItems());
            okSteps++;

            return new int[] { totalSteps, okSteps, totalSteps - okSteps };
        });
    }
}
