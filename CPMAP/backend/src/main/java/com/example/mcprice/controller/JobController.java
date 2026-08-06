package com.example.mcprice.controller;

import com.example.mcprice.dto.PageResponse;
import com.example.mcprice.dto.JobRunDto;
import com.example.mcprice.service.JobService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping("/api/jobs")
    public List<String> listJobs() {
        return jobService.listJobKeys();
    }

    @PostMapping("/api/jobs/{jobKey}/trigger")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    public JobRunDto trigger(@PathVariable String jobKey) {
        return jobService.triggerManual(jobKey);
    }

    @GetMapping("/api/jobs/{jobKey}/history")
    public PageResponse<JobRunDto> history(@PathVariable String jobKey,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(jobService.history(jobKey, page, size));
    }

    @GetMapping("/api/job-runs/{id}")
    public JobRunDto getRun(@PathVariable Long id) {
        return jobService.getRun(id);
    }

    @PostMapping("/api/job-runs/{id}/retry")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    public JobRunDto retry(@PathVariable Long id) {
        return jobService.retry(id);
    }
}
