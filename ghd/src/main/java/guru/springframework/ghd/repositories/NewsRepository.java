package guru.springframework.ghd.repositories;

import guru.springframework.ghd.dto.news.NewsProjection;
import guru.springframework.ghd.entities.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface NewsRepository extends JpaRepository<News, UUID> {
    @Query(value = """
                SELECT n.*, c.title AS categoryName 
                FROM news n 
                LEFT JOIN category c ON n.category_id = c.id
                LEFT JOIN brand b ON n.brand_id = b.id
                WHERE n.del_flag = 0
                AND (n.category_id IS NULL OR n.category_id = '')
                AND (n.brand_id IS NULL OR n.brand_id = '')
                AND n.status = 'PUBLISHED'
            """,
            countQuery = """
                    SELECT count(*) FROM news n
                    WHERE n.del_flag = 0
                    AND (n.category_id IS NULL OR n.category_id = '')
                    AND (n.brand_id IS NULL OR n.brand_id = '')
                    AND n.status = 'PUBLISHED'
                    """,
            nativeQuery = true)
    Page<NewsProjection> findAllNewsClient(Pageable pageable);

    @Query(value = """
    SELECT 
        n.*, 
        c.title AS categoryName 
    FROM news n 
    LEFT JOIN category c ON n.category_id = c.id 
    WHERE n.del_flag = 0
            AND n.status = 'PUBLISHED'
            AND n.is_featured = 1
    ORDER BY n.created_date DESC
    LIMIT 5
    """, nativeQuery = true)
    List<NewsProjection> findNewsFeatured();

    @Query(value = """
        SELECT n.*, c.title AS categoryName, b.title AS brandName 
        FROM news n 
        LEFT JOIN category c ON n.category_id = c.id 
        LEFT JOIN brand b ON n.brand_id = b.id 
        WHERE n.del_flag = 0
        """,
            countQuery = "SELECT count(*) FROM news n WHERE n.del_flag = 0",
            nativeQuery = true)
    Page<NewsProjection> findAllNewsAdmin(Pageable pageable);

    @Query(value = """
            SELECT n.*, c.title as categoryName 
            FROM news n 
            LEFT JOIN category c ON n.category_id = c.id
            LEFT JOIN brand b ON n.brand_id = b.id
            WHERE (:search IS NULL OR n.title LIKE CONCAT('%', :search, '%')) 
              AND (:status IS NULL OR n.status = :status)
              AND n.del_flag = 0
            """,
            countQuery = """
                    SELECT count(*) FROM news n 
                    WHERE (:search IS NULL OR n.title LIKE CONCAT('%', :search, '%')) 
                      AND (:status IS NULL OR n.status = :status)
                      AND n.del_flag = 0
                    """,
            nativeQuery = true)
    Page<NewsProjection> searchNews(@Param("search") String search, @Param("status") Integer status, Pageable pageable);

    @Query(value = """
            SELECT n.*, c.title as categoryName
            FROM news n
            LEFT JOIN category c ON n.category_id = c.id
            WHERE (:title IS NULL OR :title = '' OR n.title LIKE CONCAT('%', :title, '%'))
            AND n.del_flag = 0
            AND (n.category_id IS NULL OR n.category_id = '')
            AND (n.brand_id IS NULL OR n.brand_id = '')
            AND n.status = 'PUBLISHED'
            """,
            countQuery = """
                    SELECT count(*) FROM news n
                    WHERE (:title IS NULL OR :title = '' OR n.title LIKE CONCAT('%', :title, '%'))
                    AND n.del_flag = 0
                    AND (n.category_id IS NULL OR n.category_id = '')
                    AND (n.brand_id IS NULL OR n.brand_id = '')
                    AND n.status = 'PUBLISHED'
                    """,
            nativeQuery = true)
    Page<NewsProjection> findAllByTitle(String title, Pageable pageRequest);

    @Query(value = """
            SELECT n.*, c.title AS categoryName
            FROM news n
            LEFT JOIN category c ON n.category_id = c.id
            WHERE (:title IS NULL OR :title = '' OR n.title LIKE CONCAT('%', :title, '%'))
            AND (:status IS NULL OR :status = '' OR n.status LIKE CONCAT('%', :status, '%'))
            AND n.del_flag = 0
            """,
            countQuery = """
                    SELECT count(*) FROM news n
                    WHERE (:title IS NULL OR :title = '' OR n.title LIKE CONCAT('%', :title, '%'))
                    AND (:status IS NULL OR :status = '' OR n.status LIKE CONCAT('%', :status, '%'))
                    AND n.del_flag = 0
                    """,
            nativeQuery = true)
    Page<NewsProjection> findNewsAdmin(String title, String status, Pageable pageRequest);

    @Query(value="SELECT u.* FROM news u WHERE u.id = :newsId and u.del_flag = 0", nativeQuery = true)
    Optional<News> findById(@Param("newsId") String newsId);


    @Query(value="SELECT u.* FROM news u WHERE u.slug = :slug and u.del_flag = 0", nativeQuery = true)
    Optional<News> findBySlug(@Param("slug") String slug);

    @Query(value = """
            SELECT n.*, c.title AS categoryName, b.title AS brandName
            FROM news n 
            INNER JOIN category c ON n.category_id = c.id 
            INNER JOIN brand b ON n.brand_id = b.id 
            WHERE n.del_flag = 0
                AND n.status = 'PUBLISHED'
                AND c.title = :categoryTitle
                AND b.title = :brandTitle
            ORDER BY n.created_date DESC
            LIMIT 1
            """, nativeQuery = true)
    NewsProjection findByCategoryAndBrand(@Param("categoryTitle") String categoryTitle, @Param("brandTitle") String brandTitle);

    @Query(value = """
    SELECT n.*, c.title AS categoryName, b.title AS brandName
    FROM news n 
    INNER JOIN category c ON n.category_id = c.id 
    LEFT JOIN brand b ON n.brand_id = b.id 
    WHERE n.del_flag = 0
        AND n.status = 'PUBLISHED'
        AND c.title = :categoryTitle
    ORDER BY n.created_date DESC
    LIMIT 1
    """, nativeQuery = true)
    NewsProjection findByCategory(@Param("categoryTitle") String categoryTitle);

    @Query(value = """
    SELECT n.*, c.title AS categoryName, b.title AS brandName
    FROM news n 
    LEFT JOIN category c ON n.category_id = c.id 
    INNER JOIN brand b ON n.brand_id = b.id 
    WHERE n.del_flag = 0
        AND n.status = 'PUBLISHED'
        AND b.title = :brandTitle
    ORDER BY n.created_date DESC
    LIMIT 1
    """, nativeQuery = true)
    NewsProjection findByBrand(@Param("brandTitle") String brandTitle);
}
