package com.example.mcprice.repository;

import com.example.mcprice.domain.WebsitePublishItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebsitePublishItemRepository extends JpaRepository<WebsitePublishItem, Long> {

    List<WebsitePublishItem> findByWebsitePublishRunId(Long runId);

    Optional<WebsitePublishItem> findFirstByProductIdOrderByCreatedAtDesc(Long productId);
}
