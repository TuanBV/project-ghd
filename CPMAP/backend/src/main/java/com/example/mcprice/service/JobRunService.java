package com.example.mcprice.service;

import com.example.mcprice.domain.RunStatus;
import com.example.mcprice.exception.JobAlreadyRunningException;
import com.example.mcprice.domain.JobRun;
import com.example.mcprice.repository.JobRunRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quan ly vong doi JobRun. Dua vao unique index mot phan (ux_job_runs_running) tren DB de
 * dam bao hai lan chay cung job_key khong bao gio chong lan — day la nguon chan ly duy nhat,
 * khong dung lock trong memory (vi ung dung co the chay nhieu instance).
 */
@Service
@RequiredArgsConstructor
public class JobRunService {

    private static final String MDC_KEY = "correlationId";

    private final JobRunRepository jobRunRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobRun createQueued(String jobKey, String triggerType, String triggeredBy) {
        String correlationId = MDC.get(MDC_KEY) != null ? MDC.get(MDC_KEY) : UUID.randomUUID().toString();
        try {
            return jobRunRepository.save(JobRun.builder()
                    .jobKey(jobKey)
                    .triggerType(triggerType)
                    .status(RunStatus.QUEUED)
                    .correlationId(correlationId)
                    .triggeredBy(triggeredBy)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new JobAlreadyRunningException(jobKey);
        }
    }

    @Transactional
    public void markRunning(Long jobRunId) {
        JobRun run = jobRunRepository.findById(jobRunId).orElseThrow();
        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(OffsetDateTime.now());
        jobRunRepository.save(run);
    }

    @Transactional
    public void updateProgress(Long jobRunId, int totalItems, int successItems, int failedItems) {
        JobRun run = jobRunRepository.findById(jobRunId).orElseThrow();
        run.setTotalItems(totalItems);
        run.setSuccessItems(successItems);
        run.setFailedItems(failedItems);
        run.setProgressPercent(totalItems == 0 ? 100 : (int) (100.0 * (successItems + failedItems) / totalItems));
        jobRunRepository.save(run);
    }

    @Transactional
    public void markFinished(Long jobRunId, int totalItems, int successItems, int failedItems, String errorDetail) {
        JobRun run = jobRunRepository.findById(jobRunId).orElseThrow();
        run.setTotalItems(totalItems);
        run.setSuccessItems(successItems);
        run.setFailedItems(failedItems);
        run.setProgressPercent(100);
        run.setErrorDetail(errorDetail);
        RunStatus status;
        if (errorDetail != null && successItems == 0 && totalItems > 0) {
            status = RunStatus.FAILED;
        } else if (failedItems > 0) {
            status = RunStatus.PARTIAL_SUCCESS;
        } else {
            status = RunStatus.SUCCESS;
        }
        run.setStatus(status);
        run.setFinishedAt(OffsetDateTime.now());
        jobRunRepository.save(run);
    }

    @Transactional
    public void markFailed(Long jobRunId, String errorDetail) {
        JobRun run = jobRunRepository.findById(jobRunId).orElseThrow();
        run.setStatus(RunStatus.FAILED);
        run.setErrorDetail(errorDetail);
        run.setFinishedAt(OffsetDateTime.now());
        jobRunRepository.save(run);
    }
}
