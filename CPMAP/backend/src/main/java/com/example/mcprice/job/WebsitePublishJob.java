package com.example.mcprice.job;

import com.example.mcprice.domain.JobKeys;
import com.example.mcprice.service.JobRunService;
import com.example.mcprice.service.WebsitePublishService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class WebsitePublishJob extends QuartzJobBean {

    @Autowired
    private WebsitePublishService websitePublishService;
    @Autowired
    private JobRunService jobRunService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobExecutionHelper.run(context, jobRunService, JobKeys.WEBSITE_PUBLISH, jobRunId -> {
            var dto = websitePublishService.publishApprovedRecommendations(null, jobRunId);
            return new int[] { dto.totalItems(), dto.successItems(), dto.failedItems() };
        });
    }
}
