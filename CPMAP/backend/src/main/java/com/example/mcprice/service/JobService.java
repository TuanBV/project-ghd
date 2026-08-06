package com.example.mcprice.service;

import com.example.mcprice.exception.NotFoundException;
import com.example.mcprice.domain.JobKeys;
import com.example.mcprice.domain.JobRun;
import com.example.mcprice.dto.JobRunDto;
import com.example.mcprice.repository.JobRunRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Trigger job bang tay: tao JobRun QUEUED truoc, dua jobRunId vao JobDataMap de Quartz Job dung lai chinh row nay. */
@Service
@RequiredArgsConstructor
public class JobService {

    private final Scheduler scheduler;
    private final JobRunService jobRunService;
    private final JobRunRepository jobRunRepository;

    public List<String> listJobKeys() {
        return JobKeys.ALL;
    }

    public JobRunDto triggerManual(String jobKey) {
        if (!JobKeys.ALL.contains(jobKey)) {
            throw new NotFoundException("Khong biet job key: " + jobKey);
        }
        String actor = currentActor();
        JobRun run = jobRunService.createQueued(jobKey, "MANUAL", actor);
        try {
            JobDataMap dataMap = new JobDataMap(Map.of("jobRunId", run.getId()));
            scheduler.triggerJob(JobKey.jobKey(jobKey), dataMap);
        } catch (SchedulerException e) {
            jobRunService.markFailed(run.getId(), "Khong trigger duoc Quartz job: " + e.getMessage());
            throw new IllegalStateException("Khong trigger duoc job " + jobKey, e);
        }
        return toDto(run);
    }

    public JobRunDto getRun(Long id) {
        return toDto(jobRunRepository.findById(id).orElseThrow(() -> NotFoundException.of("JobRun", id)));
    }

    public org.springframework.data.domain.Page<JobRunDto> history(String jobKey, int page, int size) {
        return jobRunRepository.findByJobKeyOrderByCreatedAtDesc(jobKey, PageRequest.of(page, size)).map(this::toDto);
    }

    /** "Retry" = chay lai job (cac job deu idempotent nen re-run se tu sua cac item that bai truoc do). */
    public JobRunDto retry(Long id) {
        JobRun previous = jobRunRepository.findById(id).orElseThrow(() -> NotFoundException.of("JobRun", id));
        return triggerManual(previous.getJobKey());
    }

    private String currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "SYSTEM" : auth.getName();
    }

    private JobRunDto toDto(JobRun run) {
        return new JobRunDto(run.getId(), run.getJobKey(), run.getTriggerType(), run.getStatus().name(),
                run.getTotalItems(), run.getSuccessItems(), run.getFailedItems(), run.getProgressPercent(),
                run.getStartedAt(), run.getFinishedAt(), run.getErrorDetail(), run.getCorrelationId());
    }
}
