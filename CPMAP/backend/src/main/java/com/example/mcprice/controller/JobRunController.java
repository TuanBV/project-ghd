package com.example.mcprice.controller;

import com.example.mcprice.dto.JobRunDto;
import com.example.mcprice.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-runs")
@RequiredArgsConstructor
public class JobRunController {

    private final JobService jobService;

    @GetMapping("/{id}")
    public JobRunDto get(@PathVariable Long id) {
        return jobService.getRun(id);
    }

    /** Tao mot lan chay retry moi cho job run da that bai. */
    @PostMapping("/{id}/retries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    public JobRunDto createRetry(@PathVariable Long id) {
        return jobService.retry(id);
    }
}
