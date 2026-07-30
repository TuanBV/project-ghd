package guru.springframework.ghd.repositories;

import guru.springframework.ghd.dto.policy.ProductSummaryDTO;
import guru.springframework.ghd.dto.product.*;
import guru.springframework.ghd.entities.Product;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Query(value="SELECT u.* FROM product u WHERE u.id = :productId and u.del_flag = 0", nativeQuery = true)
    Optional<Product> findById(@Param("productId") String productId);

    @Query(value = """
            SELECT
                p.id as id, 
                p.title as title,  
                c.title as categoryName, 
                b.title as brandName,
                p.status as status,
                p.image as image,
                CASE 
                    WHEN COUNT(p.id) = 0 THEN 'Liên hệ'
                    WHEN MIN(p.price) = MAX(p.price) THEN CONCAT(FORMAT(MIN(p.price), 0, 'vi_VN'), ' VND')
                    ELSE CONCAT(FORMAT(MIN(p.price), 0, 'vi_VN'), ' - ', FORMAT(MAX(p.price), 0, 'vi_VN'), ' VND')
                END as priceRange,
                CAST(SUM(IFNULL(p.stock_qty, 0)) AS UNSIGNED) as totalStock,
                p.description as description
            FROM product p
            LEFT JOIN category c ON p.category_id = c.id
            LEFT JOIN brand b ON p.brand_id = b.id
            WHERE (:title IS NULL OR p.title LIKE CONCAT('%', :title, '%'))
              AND (:categoryId IS NULL OR p.category_id = :categoryId)
              AND (:brandId IS NULL OR p.brand_id = :brandId)
              AND (:status IS NULL OR p.status = :status)
              AND p.del_flag = 0
            GROUP BY p.id, c.title, b.title, p.description
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT p.id) FROM product p 
                    WHERE (:title IS NULL OR p.title LIKE CONCAT('%', :title, '%'))
                      AND (:categoryId IS NULL OR p.category_id = :categoryId)
                      AND (:brandId IS NULL OR p.brand_id = :brandId)
                      AND (:status IS NULL OR p.status = :status)
                      AND p.del_flag = 0
                    """,
            nativeQuery = true)
    Page<ProductProjection> findAllNative(
            @Param("title") String title,
            @Param("categoryId") String categoryId,
            @Param("brandId") String brandId,
            @Param("status") Integer status,
            Pageable pageable);

    @Query(value = """
            SELECT p.id, p.title as name
            FROM product p
            WHERE p.policy_id = :policyId 
              AND p.del_flag = 0
            """,
            nativeQuery = true)
    List<ProductSummaryDTO> findAllByPolicyId(@Param("policyId") String policyId);


    @Modifying
    @Query("UPDATE Product p SET p.groupId = NULL WHERE p.groupId = :groupId")
    void resetGroupId(@Param("groupId") String groupId);

    @Query(value = """
            SELECT p.id, p.title as name
            FROM product p
            WHERE p.group_id = :groupId 
              AND p.del_flag = 0
              AND p.id <> :currentId
            """,
            nativeQuery = true)
    List<ProductSummaryDTO> findAllByGroupId(@Param("groupId") String groupId, @Param("currentId") String currentId);

    @Modifying
    @Query(value = """
            UPDATE product p SET p.group_id = :groupId WHERE p.id IN (:ids)
            """,
            nativeQuery = true)
    void updateGroupIdForList(@Param("groupId") String groupId, @Param("ids") List<String> ids);

    @Modifying
    @Query("UPDATE Product p SET p.groupId = NULL WHERE p.id IN :ids")
    void updateGroupIdToNullForIds(@Param("ids") Collection<String> ids);

    @Query("SELECT DISTINCT p.groupId FROM Product p WHERE p.id IN :ids AND p.groupId IS NOT NULL")
    List<String> findGroupIdsByIds(@Param("ids") Collection<String> ids);

    @Query("SELECT p.id FROM Product p WHERE p.groupId = :groupId")
    List<String> findAllIdsByGroupId(@Param("groupId") String groupId);

    @Modifying
    @Query("UPDATE Product p SET p.groupId = NULL WHERE p.groupId = :groupId")
    void updateGroupIdToNull(@Param("groupId") String groupId);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE product 
            SET policy_id = :policyId, 
                updated_date = CURRENT_TIMESTAMP
            WHERE id IN (:productIdList) 
              AND del_flag = 0
            """, nativeQuery = true)
    void updatePolicyIdByProductIds(@Param("policyId") String policyId,
                                    @Param("productIdList") List<String> productIdList);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE product 
            SET policy_id = :policyId, 
                updated_date = CURRENT_TIMESTAMP
            WHERE policy_id IS NULL 
              AND del_flag = 0
            """, nativeQuery = true)
    void updatePolicyIdForNullProducts(@Param("policyId") String policyId);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE product
            SET policy_id = NULL, 
                updated_date = CURRENT_TIMESTAMP 
            WHERE policy_id = :policyId
            """, nativeQuery = true)
    void clearPolicyFromProduct(@Param("policyId") String policyId);


    @Query(value = "SELECT u.* FROM product u WHERE u.group_id = :groupId and u.del_flag = 0", nativeQuery = true)
    List<Product> findByGroupId(@Param("groupId") String groupId);

    @Query(value = """
            SELECT p.id as productId,
            p.title as title,
            p.description as description,
            c.title as categoryName,
            p.price as price,
            p.sale_price as salePrice,
            p.image as image,
            p.sold_count as soldCount,
            p.slug as slug
            FROM product p
            JOIN category c ON p.category_id = c.id
            WHERE p.id in (:listProductId)
            """,
            nativeQuery = true)
    List<ProductDetailCartResponse> findByProductId(@Param("listProductId") List<String> listProductId);

    @Query(value = """
            SELECT 
                p.id as id, 
                p.title as title, 
                MAX(c.title) as categoryName, 
                MAX(b.title) as brandName, 
                MAX(p.status) as status, 
                MAX(p.content) as content,
                MAX(p.description) as description,
                MAX(p.policy_id) as policyId,
                MAX(p.specification) as specification, 
                MAX(p.id) as productId, 
                MAX(p.sku) as sku, 
                MIN(p.price) as price, 
                MIN(p.sale_price) as salePrice, 
                MAX(p.stock_qty) as stockQty, 
                MAX(p.color) as color, 
                MAX(p.sold_count) as soldCount, 
                MAX(p.size) as size, 
                MAX(p.image) as image, 
                MAX(p.slug) as slug 
            FROM product p 
            LEFT JOIN category c ON p.category_id = c.id 
            LEFT JOIN brand b ON p.brand_id = b.id 
            WHERE p.del_flag = 0
            AND (:title IS NULL OR :title = '' OR (
                p.title LIKE CONCAT('%', :title, '%') OR
                p.sku LIKE CONCAT('%', :title, '%') OR
                p.slug LIKE CONCAT('%', :title, '%') OR
                p.color LIKE CONCAT('%', :title, '%') OR
                p.size LIKE CONCAT('%', :title, '%') OR
                c.title LIKE CONCAT('%', :title, '%') OR
                b.title LIKE CONCAT('%', :title, '%')
            ))
            GROUP BY p.id
            LIMIT 8
            """, nativeQuery = true)
    List<IProductDetailClient> findTop8Suggestions(@Param("title") String title);

    @Query(value = """
            SELECT 
                p.id as id, 
                p.title as title, 
                c.title as categoryName, 
                b.title as brandName, 
                p.status as status, 
                p.content as content,
                p.description as description,
                p.policy_id as policyId,
                p.specification as specification,
                p.sku as sku, 
                p.price as price, 
                p.sale_price as salePrice, 
                p.stock_qty as stockQty, 
                p.color as color, 
                p.size as size, 
                p.sold_count as soldCount,            
                p.image as image, 
                p.slug as slug 
            FROM product p
            LEFT JOIN category c ON p.category_id = c.id 
            LEFT JOIN brand b ON p.brand_id = b.id 
            WHERE (:title IS NULL OR :title = '' OR (
                p.title LIKE %:title% OR
                p.sku LIKE %:title% OR
                p.slug LIKE %:title% OR
                p.color LIKE %:title% OR
                p.size LIKE %:title% OR
                c.title LIKE %:title% OR
                b.title LIKE %:title%
            ))
            AND (:categoryTitle IS NULL OR :categoryTitle = '' OR c.slug = :categoryTitle)
            AND (:brandTitle IS NULL OR :brandTitle = '' OR b.slug = :brandTitle)
            AND (:status IS NULL OR p.status = :status) 
            AND (:minPrice IS NULL OR p.price >= :minPrice) 
            AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            """,
            countQuery = """
                    SELECT count(*) FROM product p 
                    LEFT JOIN category c ON p.category_id = c.id 
                    LEFT JOIN brand b ON p.brand_id = b.id 
                    WHERE (:title IS NULL OR :title = '' OR (
                        p.title LIKE %:title% OR
                        p.sku LIKE %:title% OR
                        p.slug LIKE %:title% OR
                        p.color LIKE %:title% OR
                        p.size LIKE %:title% OR
                        c.title LIKE %:title% OR
                        b.title LIKE %:title%
                    ))
                    AND (:categoryTitle IS NULL OR :categoryTitle = '' OR c.slug = :categoryTitle)
                    AND (:brandTitle IS NULL OR :brandTitle = '' OR b.slug = :brandTitle)
                    AND (:status IS NULL OR p.status = :status) 
                    AND (:minPrice IS NULL OR p.price >= :minPrice) 
                    AND (:maxPrice IS NULL OR p.price <= :maxPrice)
                    """,
            nativeQuery = true)
    Page<IProductDetailClient> searchProduct(
            String title, String categoryTitle, String brandTitle, Integer status,
            Double minPrice, Double maxPrice, Pageable pageable);


    @Query(value = """
            SELECT 
                p.id as id, 
                p.title as title, 
                c.title as categoryName, 
                b.title as brandName, 
                p.status as status, 
                p.content as content,
                p.policy_id as policyId,
                p.specification as specification,
                p.description as description,
                p.sku as sku, 
                p.price as price, 
                p.sale_price as salePrice, 
                p.stock_qty as stockQty, 
                p.color as color, 
                p.size as size, 
                p.sold_count as soldCount,            
                p.image as image, 
                p.slug as slug 
            FROM product p
            LEFT JOIN category c ON p.category_id = c.id 
            LEFT JOIN brand b ON p.brand_id = b.id 
            WHERE (:title IS NULL OR :title = '' OR (
                p.title LIKE %:title% OR
                p.sku LIKE %:title% OR
                p.slug LIKE %:title% OR
                p.color LIKE %:title% OR
                p.size LIKE %:title% OR
                c.title LIKE %:title% OR
                b.title LIKE %:title%
            ))
            AND (:categoryId IS NULL OR :categoryId = '' OR c.id = :categoryId)
            AND (:brandId IS NULL OR :brandId = '' OR b.id = :brandId)
            AND p.del_flag = 0
            """,
            nativeQuery = true)
    List<IProductDetailClient> getProductExport(String title, String categoryId, String brandId);

    @Query(value = """
            SELECT 
                p.id as id, 
                p.title as title,
                p.variant_name as variantName,
                c.id as categoryId,
                c.title as categoryName,
                b.id as brandId,
                b.title as brandName,
                p.description as description,
                p.status as status, 
                p.policy_id as policyId, 
                p.content as content, 
                p.specification as specification,
                p.sku as sku, 
                p.price as price, 
                p.sale_price as salePrice, 
                p.stock_qty as stockQty, 
                p.color as color, 
                p.sold_count as soldCount,
                p.size as size, 
                p.image as image, 
                p.slug as slug,
                p.group_id as groupId
            FROM product p 
            LEFT JOIN category c ON p.category_id = c.id 
            LEFT JOIN brand b ON p.brand_id = b.id 
            WHERE p.slug = :slug 
            LIMIT 1
            """, nativeQuery = true)
    IProductDetailClient findBySlug(@Param("slug") String slug);


    @Query(value = """
            SELECT 
                p.id as id, 
                p.title as title, 
                p.variant_name as variantName,
                c.id as categoryId,
                c.title as categoryName,
                p.description as description,
                b.id as brandId,
                b.title as brandName, 
                p.status as status, 
                p.policy_id as policyId, 
                p.content as content, 
                p.specification as specification,
                p.sku as sku, 
                p.price as price, 
                p.sale_price as salePrice, 
                p.stock_qty as stockQty, 
                p.color as color, 
                p.sold_count as soldCount,
                p.size as size, 
                p.image as image, 
                p.slug as slug,
                p.group_id as groupId
            FROM product p 
            LEFT JOIN category c ON p.category_id = c.id 
            LEFT JOIN brand b ON p.brand_id = b.id 
            WHERE p.id = :idProduct 
            LIMIT 1
            """, nativeQuery = true)
    IProductDetailClient findByIdProduct(@Param("idProduct") String idProduct);

    @Query(value =
            """
                            SELECT p.id as id,
                            p.title as title,
                            c.title as categoryName,
                            b.title as brandName,
                            p.status as status,
                            p.description as description,
                            p.content as content,
                            p.policy_id as policyId,
                            p.specification as specification,
                            p.sku as sku,
                            p.price as price,
                            p.sale_price as salePrice,
                            p.stock_qty as stockQty,
                            p.color as color,
                            p.size as size,
                            p.sold_count as soldCount,
                            p.image as image,
                            p.slug as slug
                            FROM product p
                            LEFT JOIN category c ON p.category_id = c.id
                            LEFT JOIN brand b ON p.brand_id = b.id
                            WHERE p.del_flag = 0
                            ORDER BY p.created_date DESC
                    """,
            countQuery = "SELECT count(*) FROM product p WHERE p.del_flag = 0",
            nativeQuery = true)
    Page<IProductDetailClient> getLatestProducts(Pageable pageable);

    @Query(value =
            """
                            SELECT
                            p.id as id,
                            p.title as title,
                            c.title as categoryName,
                            b.title as brandName,
                            p.description as description,
                            p.status as status,
                            p.content as content,
                            p.policy_id as policyId,
                            p.specification as specification,
                            p.sku as sku,
                            p.price as price,
                            p.sale_price as salePrice,
                            p.stock_qty as stockQty,
                            p.color as color,
                            p.size as size,
                            p.sold_count as soldCount,
                            p.image as image,
                            p.slug as slug
                            FROM product p
                            LEFT JOIN category c ON p.category_id = c.id
                            LEFT JOIN brand b ON p.brand_id = b.id
                            WHERE p.del_flag = 0
                            AND c.id = :categoryId
                            ORDER BY p.created_date DESC
                    """,
            countQuery = "SELECT count(*) FROM product p WHERE p.del_flag = 0",
            nativeQuery = true)
    Page<IProductDetailClient> getLatestProductsByCategory(String categoryId, Pageable pageable);

    @Query(value = """
            SELECT 
                p.id as id, 
                p.title as title, 
                c.id as categoryId,
                p.description as description,
                c.title as categoryName,
                b.id as brandId,
                p.policy_id as policyId,
                b.title as brandName, 
                p.status as status,
                p.price as price, 
                p.sale_price as salePrice,
                p.image as image,
                p.sold_count as soldCount,
                p.slug as slug 
            FROM product p
            LEFT JOIN category c ON p.category_id = c.id 
            LEFT JOIN brand b ON p.brand_id = b.id 
            WHERE p.del_flag = 0
            ORDER BY p.created_date DESC
            LIMIT 4
            """, nativeQuery = true)
    List<IProductDetailClient> findTop4Related();


    @Query(value = """
            SELECT 
                p.id as id, 
                p.title as name
            FROM product p
            WHERE (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND p.del_flag = 0
            ORDER BY p.title ASC
            """, nativeQuery = true)
    List<ProductSearchDTO> findByTitleContaining(@Param("keyword") String keyword);
}
