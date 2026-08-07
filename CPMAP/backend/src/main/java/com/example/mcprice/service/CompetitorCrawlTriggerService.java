package com.example.mcprice.service;

import com.example.mcprice.domain.JobRun;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Diem vao cho nut "Doi chieu SKU & tinh gia" tren trang Competitors (thay cho nut "Test crawl"
 * cu): doi chieu SKU danh sach URL da kham pha voi san pham hien co (nhu "Quet lai"), crawl gia
 * THAT cho cac listing da khop chac chan, roi tinh lai recommendation cho cac san pham lien
 * quan. Chay ngam, tra ve jobRunId de FE theo doi %.
 */
@Service
@RequiredArgsConstructor
public class CompetitorCrawlTriggerService {

    public static final String JOB_KEY_PREFIX = "CompetitorCrawlJob:";

    private final JobRunService jobRunService;
    private final CompetitorCrawlAsyncExecutor asyncExecutor;

    public Long triggerCrawlAndRecalculate(Long competitorId, String triggeredBy) {
        JobRun run = jobRunService.createQueued(JOB_KEY_PREFIX + competitorId, "MANUAL", triggeredBy);
        asyncExecutor.runCrawlAndRecalculate(competitorId, run.getId());
        return run.getId();
    }

    public static String jobKeyFor(Long competitorId) {
        return JOB_KEY_PREFIX + competitorId;
    }
}
