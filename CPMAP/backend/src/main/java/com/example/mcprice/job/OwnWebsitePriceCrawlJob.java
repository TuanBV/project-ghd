package com.example.mcprice.job;

import com.example.mcprice.domain.JobKeys;
import com.example.mcprice.service.JobRunService;
import com.example.mcprice.service.OwnWebsitePriceCrawlService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

/** Chi chay khi bam "Chay ngay" tren trang Jobs — khong nam trong DailyPipelineJob (xem OwnWebsitePriceCrawlService). */
public class OwnWebsitePriceCrawlJob extends QuartzJobBean {

    @Autowired
    private OwnWebsitePriceCrawlService ownWebsitePriceCrawlService;
    @Autowired
    private JobRunService jobRunService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobExecutionHelper.run(context, jobRunService, JobKeys.OWN_WEBSITE_PRICE_CRAWL,
                ownWebsitePriceCrawlService::crawlMissingPrices);
    }
}
