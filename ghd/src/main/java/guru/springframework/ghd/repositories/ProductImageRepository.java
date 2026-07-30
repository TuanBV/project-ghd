package guru.springframework.ghd.repositories;

import guru.springframework.ghd.entities.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    @Query(value = """
        SELECT u.*
        FROM product_image u
        WHERE u.product_id = :productId
          AND u.del_flag = 0
        ORDER BY COALESCE(u.sort_order, 999999), u.id
        """, nativeQuery = true)
    List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(String productId);

    @Query(value = """
        SELECT u.*
        FROM product_image u
        WHERE u.product_id = :productId
          AND u.del_flag = 0
        ORDER BY COALESCE(u.sort_order, 999999), u.id
        """, nativeQuery = true)
    List<ProductImage> findByProductId(String productId);

    @Query(value = """
        SELECT u.*
        FROM product_image u
        WHERE u.product_id IN (:productIds)
          AND u.del_flag = 0
        ORDER BY u.product_id, COALESCE(u.sort_order, 999999), u.id
        """, nativeQuery = true)
    List<ProductImage> findByProductIdIn(List<String> productIds);

    void deleteAllByImageUrl(String imageUrl);
}