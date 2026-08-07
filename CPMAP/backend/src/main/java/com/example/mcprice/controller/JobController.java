package com.example.mcprice.controller;

import com.example.mcprice.dto.PageResponse;
import com.example.mcprice.dto.JobRunDto;
import com.example.mcprice.service.JobService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping
    public List<String> listJobs() {
        return jobService.listJobKeys();
    }

    /** Tao mot lan chay thu cong moi cho job dinh nghia san (jobKey). */
    @PostMapping("/{jobKey}/runs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    public JobRunDto createRun(@PathVariable String jobKey) {
        return jobService.triggerManual(jobKey);
    }

    @GetMapping("/{jobKey}/runs")
    public PageResponse<JobRunDto> listRuns(@PathVariable String jobKey,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(jobService.history(jobKey, page, size));
    }
}
