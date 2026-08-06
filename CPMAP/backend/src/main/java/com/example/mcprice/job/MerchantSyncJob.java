package com.example.mcprice.job;

import com.example.mcprice.domain.JobKeys;
import com.example.mcprice.service.JobRunService;
import com.example.mcprice.service.MerchantSyncService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class MerchantSyncJob extends QuartzJobBean {

    @Autowired
    private MerchantSyncService merchantSyncService;
    @Autowired
    private JobRunService jobRunService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobExecutionHelper.run(context, jobRunService, JobKeys.MERCHANT_SYNC, jobRunId -> {
            var dto = merchantSyncService.syncApprovedRecommendations(null, jobRunId);
            return new int[] { dto.totalItems(), dto.successItems(), dto.failedItems() };
        });
    }
}
