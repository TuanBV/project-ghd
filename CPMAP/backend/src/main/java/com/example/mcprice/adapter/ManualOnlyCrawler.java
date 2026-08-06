package com.example.mcprice.adapter;

import com.example.mcprice.domain.Competitor;
import com.example.mcprice.domain.CrawlMode;
import com.example.mcprice.dto.CrawlResult;
import com.example.mcprice.domain.CompetitorListing;
import org.springframework.stereotype.Component;

/** Doi thu duoc cau hinh MANUAL_ONLY: khong bao gio goi network, luon yeu cau nhap tay. */
@Component
public class ManualOnlyCrawler implements CompetitorPriceCrawler {

    @Override
    public boolean supports(Competitor competitor) {
        return competitor.getCrawlMode() == CrawlMode.MANUAL_ONLY;
    }

    @Override
    public CrawlResult crawl(CompetitorListing listing) {
        return CrawlResult.manualRequired("Doi thu '" + listing.getCompetitor().getName()
                + "' duoc cau hinh MANUAL_ONLY, can nhap gia tay");
    }
}
