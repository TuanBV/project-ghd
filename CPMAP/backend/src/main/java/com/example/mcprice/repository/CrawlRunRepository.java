package com.example.mcprice.repository;

import com.example.mcprice.domain.CrawlRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlRunRepository extends JpaRepository<CrawlRun, Long> {
}
