package com.example.mcprice.repository;

import com.example.mcprice.domain.PriceRecommendation;
import com.example.mcprice.domain.RecommendationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceRecommendationRepository extends JpaRepository<PriceRecommendation, Long> {

    Optional<PriceRecommendation> findFirstByProductIdOrderByCreatedAtDesc(Long productId);

    @Query("select r from PriceRecommendation r where r.product.id in :productIds "
            + "and r.createdAt = (select max(r2.createdAt) from PriceRecommendation r2 where r2.product.id = r.product.id)")
    List<PriceRecommendation> findLatestForProducts(@Param("productIds") List<Long> productIds);

    @Query(value = "select r from PriceRecommendation r join fetch r.product where r.status = :status",
            countQuery = "select count(r) from PriceRecommendation r where r.status = :status")
    Page<PriceRecommendation> findByStatus(@Param("status") RecommendationStatus status, Pageable pageable);

    long countByStatus(RecommendationStatus status);

    @Query(value = "SELECT DISTINCT ON (product_id) * FROM price_recommendations ORDER BY product_id, created_at DESC",
            nativeQuery = true)
    List<PriceRecommendation> findLatestPerProduct();
}
