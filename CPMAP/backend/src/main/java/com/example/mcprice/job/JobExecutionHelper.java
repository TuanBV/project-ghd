package com.example.mcprice.job;

import com.example.mcprice.domain.JobRun;
import com.example.mcprice.service.JobRunService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Khung chung cho moi Quartz Job: lay/tao JobRun, chuyen QUEUED -> RUNNING -> SUCCESS/PARTIAL_SUCCESS/FAILED,
 * dam bao dung mot JobRun duy nhat cho ca truong hop trigger tay (jobRunId co san trong JobDataMap tu
 * JobService.triggerManual) va truong hop Quartz tu chay theo cron (tu tao JobRun moi voi trigger_type=SCHEDULED).
 */
final class JobExecutionHelper {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionHelper.class);

    interface Work {
        int[] run(Long jobRunId) throws Exception;
    }

    private JobExecutionHelper() {
    }

    static void run(JobExecutionContext context, JobRunService jobRunService, String jobKey, Work work)
            throws JobExecutionException {
        Long jobRunId = (Long) context.getMergedJobDataMap().get("jobRunId");
        boolean ownsJobRun = jobRunId == null;
        if (ownsJobRun) {
            JobRun run;
            try {
                run = jobRunService.createQueued(jobKey, "SCHEDULED", "QUARTZ_SCHEDULER");
            } catch (RuntimeException e) {
                log.info("Bo qua lan chay theo cron cua {} vi da co job dang chay: {}", jobKey, e.getMessage());
                return;
            }
            jobRunId = run.getId();
        }
        jobRunService.markRunning(jobRunId);
        try {
            int[] result = work.run(jobRunId);
            jobRunService.markFinished(jobRunId, result[0], result[1], result[2], null);
        } catch (Exception e) {
            log.error("Job {} (run #{}) that bai: {}", jobKey, jobRunId, e.getMessage(), e);
            jobRunService.markFailed(jobRunId, e.getMessage());
            throw new JobExecutionException(e);
        }
    }
}
