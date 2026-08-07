package com.example.mcprice.controller;

import com.example.mcprice.exception.NotFoundException;
import com.example.mcprice.domain.CrawlRun;
import com.example.mcprice.dto.CrawlRunDto;
import com.example.mcprice.dto.RunCrawlRequest;
import com.example.mcprice.repository.CrawlRunRepository;
import com.example.mcprice.service.CrawlOrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crawls")
@RequiredArgsConstructor
public class CrawlController {

    private final CrawlOrchestrationService crawlOrchestrationService;
    private final CrawlRunRepository crawlRunRepository;

    /** Tao mot CrawlRun moi (quet gia tat ca hoac mot so doi thu chi dinh). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    public CrawlRunDto create(@RequestBody(required = false) RunCrawlRequest request) {
        return crawlOrchestrationService.runCrawl(request == null ? null : request.competitorIds(), null, "MANUAL");
    }

    @GetMapping("/{runId}")
    public CrawlRunDto get(@PathVariable Long runId) {
        CrawlRun run = crawlRunRepository.findById(runId).orElseThrow(() -> NotFoundException.of("CrawlRun", runId));
        return new CrawlRunDto(run.getId(), run.getStatus().name(), run.getTotalItems(), run.getSuccessItems(),
                run.getFailedItems(), run.getStartedAt(), run.getFinishedAt());
    }
}
