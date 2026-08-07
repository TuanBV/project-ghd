package com.example.mcprice.repository;

import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.domain.MatchStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompetitorListingRepository extends JpaRepository<CompetitorListing, Long> {

    List<CompetitorListing> findByProductId(Long productId);

    @Query("select cl from CompetitorListing cl join fetch cl.product join fetch cl.competitor "
            + "where cl.competitor.id = :competitorId")
    Page<CompetitorListing> findByCompetitorId(@Param("competitorId") Long competitorId, Pageable pageable);

    @Query(value = "select cl from CompetitorListing cl join fetch cl.product join fetch cl.competitor "
            + "where cl.competitor.id = :competitorId and lower(cl.product.skuOriginal) like lower(concat('%', :sku, '%'))",
            countQuery = "select count(cl) from CompetitorListing cl "
                    + "where cl.competitor.id = :competitorId and lower(cl.product.skuOriginal) like lower(concat('%', :sku, '%'))")
    Page<CompetitorListing> findByCompetitorIdAndSkuContaining(@Param("competitorId") Long competitorId,
                                                                @Param("sku") String sku, Pageable pageable);

    /**
     * Tim URL ung cu vien cho 1 san pham theo SKU, quet TOAN BO he thong (moi doi thu, moi san
     * pham) — khop hoac theo SKU cua san pham dang gan, hoac theo chinh chuoi URL (URL thuong
     * chua SKU trong slug). Loai tru nhung listing DANG THUC SU gan cho chinh san pham dang xem
     * (trang thai nam trong activeStatuses) — khong can goi y lai cai da co san trong danh sach.
     * Listing da bi "Xoa" (REJECTED) cua CHINH san pham nay van duoc phep hien lai trong ket qua
     * tim kiem, de nguoi dung co the them lai neu xoa nham.
     */
    @Query("select cl from CompetitorListing cl join fetch cl.product join fetch cl.competitor "
            + "where not (cl.product.id = :productId and cl.matchStatus in :activeStatuses) and "
            + "(lower(cl.product.skuOriginal) like lower(concat('%', :sku, '%')) or lower(cl.url) like lower(concat('%', :sku, '%')))")
    List<CompetitorListing> findCandidatesBySkuExcludingProduct(@Param("productId") Long productId,
                                                                 @Param("sku") String sku,
                                                                 @Param("activeStatuses") List<MatchStatus> activeStatuses,
                                                                 Pageable pageable);

    long countByCompetitorId(Long competitorId);

    @Query("select distinct cl.product.id from CompetitorListing cl where cl.competitor.id = :competitorId")
    List<Long> findDistinctProductIdsByCompetitorId(@Param("competitorId") Long competitorId);

    List<CompetitorListing> findAllByCompetitorId(Long competitorId);

    List<CompetitorListing> findByProductIdIn(List<Long> productIds);

    List<CompetitorListing> findByCompetitorIdAndActiveTrueAndMatchStatusIn(Long competitorId, List<MatchStatus> statuses);

    Optional<CompetitorListing> findByCompetitorIdAndUrl(Long competitorId, String url);

    List<CompetitorListing> findByProductIdAndActiveTrue(Long productId);

    @Query("select cl from CompetitorListing cl where cl.active = true and cl.competitor.enabled = true "
            + "and cl.matchStatus in :statuses and cl.product.id = :productId")
    List<CompetitorListing> findConfirmedListingsForProduct(@Param("productId") Long productId,
                                                             @Param("statuses") List<MatchStatus> statuses);

    long countByMatchStatus(MatchStatus matchStatus);

    @Query("select count(distinct cl.product.id) from CompetitorListing cl where cl.matchStatus in :statuses")
    long countDistinctProductsByMatchStatusIn(@Param("statuses") List<MatchStatus> statuses);

    @Query("select cl.url, count(cl) from CompetitorListing cl group by cl.url having count(cl) > 1")
    List<Object[]> findDuplicateUrlGroups();

    @Query("select cl.competitor.name, count(cl) from CompetitorListing cl where cl.matchStatus in :statuses "
            + "and cl.active = true group by cl.competitor.name")
    List<Object[]> countConfirmedByCompetitor(@Param("statuses") List<MatchStatus> statuses);
}
