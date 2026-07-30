package guru.springframework.ghd.repositories;

import guru.springframework.ghd.dto.product.ProductDetailCartResponse;
import guru.springframework.ghd.dto.review.IReviewResponse;
import guru.springframework.ghd.dto.review.ReviewResponse;
import guru.springframework.ghd.entities.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query(value = """
        SELECT 
            r.id,
            r.product_id,
            r.review_name,
            r.content,
            r.rating
        FROM review r
        WHERE r.product_id = :productId
        """,
            nativeQuery = true)
    List<IReviewResponse> findByProductId(@Param("productId") String productId);
}