package com.example.mcprice.domain;

import java.util.List;

public final class JobKeys {

    public static final String IMPORT_REFRESH = "ImportRefreshJob";
    public static final String COMPETITOR_DISCOVERY = "CompetitorDiscoveryJob";
    public static final String COMPETITOR_CRAWL = "CompetitorCrawlJob";
    public static final String PRICE_CALCULATION = "PriceCalculationJob";
    public static final String WEBSITE_PUBLISH = "WebsitePublishJob";
    public static final String MERCHANT_SYNC = "MerchantSyncJob";
    public static final String STALE_OBSERVATION_CLEANUP = "StaleObservationCleanupJob";
    public static final String DAILY_PIPELINE = "DailyPipelineJob";

    public static final List<String> ALL = List.of(IMPORT_REFRESH, COMPETITOR_DISCOVERY, COMPETITOR_CRAWL,
            PRICE_CALCULATION, WEBSITE_PUBLISH, MERCHANT_SYNC, STALE_OBSERVATION_CLEANUP, DAILY_PIPELINE);

    private JobKeys() {
    }
}
