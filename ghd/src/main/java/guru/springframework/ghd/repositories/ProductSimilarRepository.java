package guru.springframework.ghd.repositories;

import guru.springframework.ghd.entities.ProductSimilar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ProductSimilarRepository extends JpaRepository<ProductSimilar, String> {

    @Query(value = """
            SELECT 
                ps.product_group
            FROM product_similar ps
            WHERE ps.id = :productSimilarId
            """, nativeQuery = true)
    Optional<String> findByProductSimilarId(@Param("productSimilarId") String productSimilarId);
    @Modifying
    @Query("DELETE FROM ProductSimilar ps WHERE ps.id NOT IN :activeIds")
    void deleteByGroupIdNotIn(@Param("activeIds") List<String> activeIds);
}