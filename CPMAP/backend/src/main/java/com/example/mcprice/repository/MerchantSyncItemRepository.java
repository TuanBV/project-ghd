package com.example.mcprice.repository;

import com.example.mcprice.domain.MerchantSyncItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantSyncItemRepository extends JpaRepository<MerchantSyncItem, Long> {
    List<MerchantSyncItem> findByMerchantSyncRunId(Long runId);

    long countByStatus(MerchantSyncItem.Status status);
}
