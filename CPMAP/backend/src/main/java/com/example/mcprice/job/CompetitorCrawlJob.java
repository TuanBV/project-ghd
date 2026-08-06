package com.example.mcprice.job;

import com.example.mcprice.service.CrawlOrchestrationService;
import com.example.mcprice.domain.JobKeys;
import com.example.mcprice.service.JobRunService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class CompetitorCrawlJob extends QuartzJobBean {

    @Autowired
    private CrawlOrchestrationService crawlOrchestrationService;
    @Autowired
    private JobRunService jobRunService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobExecutionHelper.run(context, jobRunService, JobKeys.COMPETITOR_CRAWL, jobRunId -> {
            var dto = crawlOrchestrationService.runCrawl(null, jobRunId, "SCHEDULED");
            return new int[] { dto.totalItems(), dto.successItems(), dto.failedItems() };
        });
    }
}
