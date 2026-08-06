package com.example.mcprice.controller;

import com.example.mcprice.exception.NotFoundException;
import com.example.mcprice.domain.CrawlRun;
import com.example.mcprice.dto.CrawlResult;
import com.example.mcprice.dto.CrawlRunDto;
import com.example.mcprice.repository.CrawlRunRepository;
import com.example.mcprice.service.CrawlOrchestrationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CrawlController {

    private final CrawlOrchestrationService crawlOrchestrationService;
    private final CrawlRunRepository crawlRunRepository;

    public record RunCrawlRequest(List<Long> competitorIds) {
    }

    public record TestCrawlRequest(String url) {
    }

    @PostMapping("/api/crawls/run")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    public CrawlRunDto run(@RequestBody(required = false) RunCrawlRequest request) {
        return crawlOrchestrationService.runCrawl(request == null ? null : request.competitorIds(), null, "MANUAL");
    }

    @GetMapping("/api/crawls/{runId}")
    public CrawlRunDto get(@PathVariable Long runId) {
        CrawlRun run = crawlRunRepository.findById(runId).orElseThrow(() -> NotFoundException.of("CrawlRun", runId));
        return new CrawlRunDto(run.getId(), run.getStatus().name(), run.getTotalItems(), run.getSuccessItems(),
                run.getFailedItems(), run.getStartedAt(), run.getFinishedAt());
    }

    @PostMapping("/api/competitors/{id}/test-crawl")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    public CrawlResult testCrawl(@PathVariable Long id, @RequestBody TestCrawlRequest request) {
        return crawlOrchestrationService.testCrawl(id, request.url());
    }
}
