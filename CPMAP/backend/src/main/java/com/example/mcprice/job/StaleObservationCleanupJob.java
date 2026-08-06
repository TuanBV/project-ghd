package com.example.mcprice.job;

import com.example.mcprice.domain.JobKeys;
import com.example.mcprice.service.JobRunService;
import com.example.mcprice.service.StaleObservationCleanupService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class StaleObservationCleanupJob extends QuartzJobBean {

    @Autowired
    private StaleObservationCleanupService staleObservationCleanupService;
    @Autowired
    private JobRunService jobRunService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobExecutionHelper.run(context, jobRunService, JobKeys.STALE_OBSERVATION_CLEANUP, jobRunId -> {
            int count = staleObservationCleanupService.markStaleObservations();
            return new int[] { count, count, 0 };
        });
    }
}
