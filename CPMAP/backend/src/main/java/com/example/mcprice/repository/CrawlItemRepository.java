package com.example.mcprice.repository;

import com.example.mcprice.domain.CrawlItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CrawlItemRepository extends JpaRepository<CrawlItem, Long> {
    List<CrawlItem> findByCrawlRunId(Long crawlRunId);

    List<CrawlItem> findByCrawlRunIdAndStatus(Long crawlRunId, CrawlItem.Status status);

    @Query("select cl.competitor.name, ci.status, count(ci) from CrawlItem ci join ci.competitorListing cl "
            + "group by cl.competitor.name, ci.status")
    List<Object[]> countByCompetitorAndStatus();

    @Query(value = "SELECT to_char(created_at, 'YYYY-MM-DD') as d, status, count(*) FROM crawl_items "
            + "GROUP BY d, status ORDER BY d", nativeQuery = true)
    List<Object[]> countByDateAndStatus();
}
