package com.example.mcprice.adapter;

import com.example.mcprice.domain.Competitor;
import com.example.mcprice.dto.CrawlResult;
import com.example.mcprice.domain.CompetitorListing;

/** Strategy/Adapter cho tung kieu crawl (STATIC_HTML, BROWSER, MANUAL_ONLY). */
public interface CompetitorPriceCrawler {

    boolean supports(Competitor competitor);

    CrawlResult crawl(CompetitorListing listing);
}
