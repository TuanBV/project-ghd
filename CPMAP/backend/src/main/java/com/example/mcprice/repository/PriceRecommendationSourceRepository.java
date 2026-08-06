package com.example.mcprice.repository;

import com.example.mcprice.domain.PriceRecommendationSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRecommendationSourceRepository extends JpaRepository<PriceRecommendationSource, Long> {
    List<PriceRecommendationSource> findByRecommendationId(Long recommendationId);
}
