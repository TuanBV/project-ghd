package com.example.mcprice.repository;

import com.example.mcprice.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySkuNormalized(String skuNormalized);

    /**
     * sku_normalized (bat nguon tu item_group_id cua feed MC) KHONG unique — nhieu bien the
     * (mau sac, dung luong khac nhau) co the chia se cung mot item_group_id (xem docs/data-analysis.md).
     * Dung ban nay khi can xu ly an toan truong hop trung, tranh IncorrectResultSizeDataAccessException.
     */
    List<Product> findAllBySkuNormalized(String skuNormalized);

    Optional<Product> findByProductUrl(String productUrl);

    Optional<Product> findByMcOfferId(String mcOfferId);

    List<Product> findByProductUrlIn(List<String> urls);

    long countByActiveTrue();

    @Query("select count(p) from Product p where not exists (select 1 from CompetitorListing cl where cl.product = p)")
    long countProductsWithoutAnyListing();
}
